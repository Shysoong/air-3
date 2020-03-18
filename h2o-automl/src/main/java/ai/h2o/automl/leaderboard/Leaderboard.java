package ai.h2o.automl.leaderboard;

import ai.h2o.automl.events.EventLog;
import ai.h2o.automl.events.EventLogEntry.Stage;
import hex.*;
import water.*;
import water.automl.api.LeaderboardsHandler;
import water.exceptions.H2OIllegalArgumentException;
import water.fvec.Frame;
import water.util.*;

import java.util.*;
import java.util.stream.Stream;


/**
 * Utility to track all the models built for a given dataset type.
 * <p>
 * Note that if a new Leaderboard is made for the same project_name it'll
 * keep using the old model list, which allows us to run AutoML multiple
 * times and keep adding to the leaderboard.
 * <p>
 * The models are returned sorted by either an appropriate default metric
 * for the model category (auc, mean per class error, or mean residual deviance),
 * or by a metric that's set via #setMetricAndDirection.
 * <p>
 * TODO: make this robust against removal of models from the DKV.
 */
public class Leaderboard extends Lockable<Leaderboard> {

  /**
   * @param project_name
   * @return a Leaderboard id for the project name
   */
  public static String idForProject(String project_name) { return "AutoML_Leaderboard_" + project_name; }

  /**
   * @param metric
   * @return true iff the metric is a loss function
   */
  public static boolean isLossFunction(String metric) {
    return metric != null && !Arrays.asList("auc", "aucpr").contains(metric.toLowerCase());
  }

  /**
   * Retrieves a leaderboard from DKV or creates a fresh one and add it to DKV.
   *
   * Note that if the leaderboard is reused to add new models, we have to use the same leaderboard frame.
   *
   * IMPORTANT!
   * if the leaderboard is created without leaderboardFrame, the models will be sorted according to their default metrics
   * (in order of availability: cross-validation metrics, validation metrics, training metrics).
   * Therefore, if some models were trained with/without cross-validation, or with different training or validation frames,
   * then we can't guarantee the fairness of the leaderboard ranking.
   *
   * @param project_name
   * @param eventLog
   * @param leaderboardFrame
   * @param sort_metric
   * @return an existing leaderboard if there's already one in DKV for this project, or a new leaderboard added to DKV.
   */
  public static Leaderboard getOrMake(String project_name, EventLog eventLog, Frame leaderboardFrame, String sort_metric) {
    Leaderboard leaderboard = DKV.getGet(Key.make(idForProject(project_name)));
    if (null != leaderboard) {
      if (leaderboardFrame != null
              && (!leaderboardFrame._key.equals(leaderboard._leaderboard_frame_key)
                          || leaderboardFrame.checksum() != leaderboard._leaderboard_frame_checksum)) {
        throw new H2OIllegalArgumentException("Cannot use leaderboard "+project_name+" with a new leaderboard frame"
                +" (existing leaderboard frame: "+leaderboard._leaderboard_frame_key+").");
      } else {
        eventLog.warn(Stage.Workflow, "New models will be added to existing leaderboard "+project_name
                +" (leaderboard frame="+leaderboard._leaderboard_frame_key+") with already "+leaderboard.getModelKeys().length+" models.");
      }
      if (sort_metric != null && !sort_metric.equals(leaderboard._sort_metric)) {
        leaderboard._sort_metric = sort_metric.toLowerCase();
        if (leaderboard.getLeader() != null) leaderboard.setDefaultMetrics(leaderboard.getLeader()); //reinitialize
      }
    } else {
      leaderboard = new Leaderboard(project_name, eventLog, leaderboardFrame, sort_metric);
    }
    DKV.put(leaderboard);
    return leaderboard;
  }

  /**
   * Identifier for models that should be grouped together in the leaderboard
   * (e.g., "airlines" and "iris").
   */
  private final String _project_name;

  /**
   * List of models for this leaderboard, sorted by metric so that the best is first,
   * according to the standard metric for the given model type.
   * <p>
   * Updated inside addModels().
   */
  private Key<Model>[] _model_keys = new Key[0];

  /**
   * Leaderboard/test set ModelMetrics objects for the models.
   * <p>
   * Updated inside addModels().
   */
  private final IcedHashMap<Key<ModelMetrics>, ModelMetrics> _leaderboard_model_metrics = new IcedHashMap<>();

  /**
   * Map providing for a given metric name, the list of metric values in the same order as the models
   */
  private IcedHashMap<String, double[]> _metric_values = new IcedHashMap<>();


  private LeaderboardExtensionsProvider _extensionsProvider;

  /**
   * Map listing the leaderboard extensions per model
   */
  private LeaderboardCell[] _extensions_cells = new LeaderboardCell[0];

  /**
   * Metric used to sort this leaderboard.
   */
  private String _sort_metric;

  /**
   * Metrics reported in leaderboard
   * Regression metrics: mean_residual_deviance, rmse, mse, mae, rmsle
   * Binomial metrics: auc, logloss, aucpr, mean_per_class_error, rmse, mse
   * Multinomial metrics: logloss, mean_per_class_error, rmse, mse
   */
  private String[] _metrics;

  /**
   * The eventLog attached to same AutoML instance as this Leaderboard object.
   */
  private final Key<EventLog> _eventlog_key;

  /**
   * Frame for which we return the metrics, by default.
   */
  private final Key<Frame> _leaderboard_frame_key;

  /**
   * Checksum for the Frame for which we return the metrics, by default.
   */
  private final long _leaderboard_frame_checksum;

  /**
   * Constructs a new leaderboard (doesn't put it in DKV).
   * @param project_name
   * @param eventLog
   * @param leaderboardFrame
   * @param sort_metric
   */
  public Leaderboard(String project_name, EventLog eventLog, Frame leaderboardFrame, String sort_metric) {
    super(Key.make(idForProject(project_name)));
    _project_name = project_name;
    _eventlog_key = eventLog._key;
    _leaderboard_frame_key = leaderboardFrame == null ? null : leaderboardFrame._key;
    _leaderboard_frame_checksum = leaderboardFrame == null ? 0 : leaderboardFrame.checksum();
    _sort_metric = sort_metric == null ? null : sort_metric.toLowerCase();
  }

  /**
   * Assign a {@link LeaderboardExtensionsProvider} to this leaderboard instance.
   * @param provider the provider used to generate the optional extension columns from the leaderboard.
   * @see LeaderboardExtensionsProvider
   */
  public void setExtensionsProvider(LeaderboardExtensionsProvider provider) {
    _extensionsProvider = provider;
  }

  public String getProject() {
    return _project_name;
  }

  /**
   * If no sort metric is provided when creating the leaderboard,
   * then a default sort metric will be automatically chosen based on the problem type:
   * <li>
   *     <ul>binomial classification: auc</ul>
   *     <ul>multinomial classification: logloss</ul>
   *     <ul>regression: mean_residual_deviance</ul>
   * </li>
   * @return the metric used to sort the models in the leaderboard.
   */
  public String getSortMetric() {
    return _sort_metric;
  }

  /**
   * The sort metric is always the first element in the list of metrics.
   *
   * @return the full list of metrics available in the leaderboard.
   */
  public String[] getMetrics() {
    return _metrics;
  }

  /**
   * Note: If no leaderboard was provided, then the models are sorted according to metrics obtained during training
   * in the following priority order depending on availability:
   * <li>
   *     <ol>cross-validation metrics</ol>
   *     <ol>validation metrics</ol>
   *     <ol>training metrics</ol>
   * </li>
   * @return the frame (if any) used to score the models in the leaderboard.
   */
  public Frame leaderboardFrame() {
    return _leaderboard_frame_key == null ? null : _leaderboard_frame_key.get();
  }

  /**
   * @return list of keys of models sorted by the default metric for the model category, fetched from the DKV
   */
  public Key<Model>[] getModelKeys() {
    return _model_keys;
  }

  /** Return the number of models in this Leaderboard. */
  public int getModelCount() { return getModelKeys() == null ? 0 : getModelKeys().length; }

  /**
   * @return list of models sorted by the default metric for the model category
   */
  public Model[] getModels() {
    if (getModelCount() == 0) return new Model[0];
    return getModelsFromKeys(getModelKeys());
  }

  /**
   * @return list of models sorted by the given metric
   */
  public Model[] getModelsSortedByMetric(String metric) {
    if (getModelCount() == 0) return new Model[0];
    return getModelsFromKeys(sortModels(metric));
  }

  /**
   * @return the model with the best sort metric value.
   * @see #getSortMetric()
   */
  public Model getLeader() {
    if (getModelCount() == 0) return null;
    return getModelKeys()[0].get();
  }

  /**
   * @param modelKey
   * @return the rank for the given model key, according to the sort metric ranking (leader has rank 1).
   */
  public int getModelRank(Key<Model> modelKey) {
    return ArrayUtils.find(getModelKeys(), modelKey) + 1;
  }

  /**
   * @return the ordered values (asc or desc depending if sort metric is a loss function or not) for the sort metric.
   * @see #getSortMetric()
   * @see #isLossFunction(String)
   */
  public double[] getSortMetricValues() {
    return _sort_metric == null ? null : _metric_values.get(_sort_metric);
  }

  private EventLog eventLog() { return _eventlog_key.get(); }

  private void setDefaultMetrics(Model m) {
    String[] metrics = defaultMetricsForModel(m);
    if (_sort_metric == null) {
      _sort_metric = metrics.length > 0 ? metrics[0] : "mse"; // default to a metric "universally" available
    }
    // ensure metrics is ordered in such a way that sortMetric is the first metric, and without duplicates.
    int sortMetricIdx = ArrayUtils.find(metrics, _sort_metric);
    if (sortMetricIdx > 0) {
      metrics = ArrayUtils.remove(metrics, sortMetricIdx);
      metrics = ArrayUtils.prepend(metrics, _sort_metric);
    } else if (sortMetricIdx < 0){
      metrics = ArrayUtils.append(new String[]{_sort_metric}, metrics);
    }
    _metrics = metrics;
  }

  /**
   * Add the given models to the leaderboard.
   * Note that to make this easier to use from Grid, which returns its models in random order,
   * we allow the caller to add the same model multiple times and we eliminate the duplicates here.
   * @param models
   */
  public void addModels(final Key<Model>[] models) {
    if (null == _key)
      throw new H2OIllegalArgumentException("Can't add models to a Leaderboard which isn't in the DKV.");

    // This can happen if a grid or model build timed out:
    if (null == models || models.length == 0) {
      return;
    }

    final Key<Model>[] oldModelKeys = _model_keys;
    final Key<Model> oldLeaderKey = (oldModelKeys == null || 0 == oldModelKeys.length) ? null : oldModelKeys[0];

    // eliminate duplicates
    final Set<Key<Model>> uniques = new HashSet<>(Arrays.asList(ArrayUtils.append(oldModelKeys, models)));
    final List<Key<Model>> allModelKeys = new ArrayList<>(uniques);
    final Set<Key<Model>> newModelKeys = new HashSet<>(uniques);
    newModelKeys.removeAll(Arrays.asList(oldModelKeys));

    Model model = null;
    final Frame leaderboardFrame = leaderboardFrame();
    final List<ModelMetrics> modelMetrics = new ArrayList<>();
    final Map<Key<Model>, LeaderboardCell[]> newExtensions = new HashMap<>();

    for (Key<Model> modelKey : allModelKeys) {  // fully rebuilding modelMetrics, so we loop through all keys, not only new ones
      model = modelKey.get();
      if (model == null) {
        eventLog().warn(Stage.ModelTraining, "Model in the leaderboard has unexpectedly been deleted from H2O: " + modelKey);
        continue;
      }

      if (_extensionsProvider != null && newModelKeys.contains(modelKey)) {
        newExtensions.put(modelKey, _extensionsProvider.createExtensions(model));
      }

      // If leaderboardFrame is null, use default model metrics instead
      ModelMetrics mm;
      if (leaderboardFrame == null) {
        mm = ModelMetrics.defaultModelMetrics(model);
      } else {
        mm = ModelMetrics.getFromDKV(model, leaderboardFrame);
        if (mm == null) {
          //scores and magically stores the metrics where we're looking for it on the next line
          // optimization: as we need to score leaderboard, score from the scoring time extension if provided.
          LeaderboardCell scoringTimePerRow = getExtension(modelKey, ScoringTimePerRow.COLUMN.getName(), newExtensions);
          if (scoringTimePerRow != null && scoringTimePerRow.getValue() == null) {
            scoringTimePerRow.fetch();
            mm = ModelMetrics.getFromDKV(model, leaderboardFrame);
          }
        }
        if (mm == null) { // last resort
          model.score(leaderboardFrame).delete();
          mm = ModelMetrics.getFromDKV(model, leaderboardFrame);
        }
      }
      modelMetrics.add(mm);
    }

    write_lock(); //no job/key needed as currently the leaderboard instance can only be updated by its corresponding AutoML job (otherwise, would need to pass a job param to addModels)
    if (_metrics == null) {
      // lazily set to default for this model category
      setDefaultMetrics(models[0].get());
    }

    for (ModelMetrics mm : modelMetrics) {
      if (mm != null) _leaderboard_model_metrics.put(mm._key, mm);
    }
    // Sort by metric on the leaderboard/test set or default model metrics.
    final List<Key<Model>> sortedModelKeys;
    boolean sortDecreasing = !isLossFunction(_sort_metric);
    try {
      if (leaderboardFrame == null) {
        sortedModelKeys = ModelMetrics.sortModelsByMetric(_sort_metric, sortDecreasing, allModelKeys);
      } else {
        sortedModelKeys = ModelMetrics.sortModelsByMetric(leaderboardFrame, _sort_metric, sortDecreasing, allModelKeys);
      }
    } catch (H2OIllegalArgumentException e) {
      Log.warn("ModelMetrics.sortModelsByMetric failed: " + e);
      throw e;
    }

    final Key<Model>[] sortedModelKeysArr = sortedModelKeys.toArray(new Key[0]);
    final Model[] sortedModels = getModelsFromKeys(sortedModelKeysArr);
    // now, we can update leaderboard public state
    // (tried to narrow scope of write lock, but there are still private state mutations above: _leaderboard_set_metrics + all attributes set by setDefaultMetricAndDirection)
    for (String metric : _metrics) {
      _metric_values.put(metric, getMetrics(metric, sortedModels));
    }
    _model_keys = sortedModelKeysArr;

    newExtensions.forEach(this::addExtensions);
    update();
    unlock();

    if (oldLeaderKey == null || !oldLeaderKey.equals(sortedModelKeysArr[0])) {
      eventLog().info(Stage.ModelTraining,
              "New leader: "+sortedModelKeysArr[0]+", "+ _sort_metric +": "+ _metric_values.get(_sort_metric)[0]);
    }
  } // addModels


  /**
   * @see #addModels(Key[])
   */
  @SuppressWarnings("unchecked")
  public <M extends Model> void addModel(final Key<M> key) {
    if (key == null) return;
    addModels(new Key[] {key});
  }

  private <M extends Model> void addExtensions(final Key<M> key, LeaderboardCell... extensions) {
    if (key == null) return;
    assert ArrayUtils.contains(_model_keys, key);
    assert Stream.of(extensions).allMatch(le -> getExtension(key, le.getColumn().getName()) == null);
    _extensions_cells = ArrayUtils.append(_extensions_cells, extensions);
  }

  private <M extends Model> LeaderboardCell[] getExtensions(final Key<M> key) {
    return Stream.of(_extensions_cells)
            .filter(c -> c.getModelId().equals(key))
            .toArray(LeaderboardCell[]::new);
  }

  private <M extends Model> LeaderboardCell getExtension(final Key<M> key, String extName) {
    return getExtension(key, extName, Collections.singletonMap((Key<Model>)key, getExtensions(key)));
  }

  private <M extends Model> LeaderboardCell getExtension(final Key<M> key, String extName, Map<Key<Model>, LeaderboardCell[]> extensions) {
    if (extensions != null && extensions.containsKey(key)) {
      return Stream.of(extensions.get(key))
              .filter(le -> le.getColumn().getName().equals(extName))
              .findFirst()
              .orElse(null);
    }
    return null;
  }

  private static Model[] getModelsFromKeys(Key<Model>[] modelKeys) {
    Model[] models = new Model[modelKeys.length];
    int i = 0;
    for (Key<Model> modelKey : modelKeys)
      models[i++] = DKV.getGet(modelKey);
    return models;
  }

  /**
   * @return list of keys of models sorted by the given metric, fetched from the DKV
   */
  private Key<Model>[] sortModels(String metric) {
    Key<Model>[] models = getModelKeys();
    boolean decreasing = !isLossFunction(metric);
    List<Key<Model>> newModelsSorted =
            ModelMetrics.sortModelsByMetric(metric, decreasing, Arrays.asList(models));
    return newModelsSorted.toArray(new Key[0]);
  }

  private double[] getMetrics(String metric, Model[] models) {
    double[] metrics = new double[models.length];
    int i = 0;
    Frame leaderboardFrame = leaderboardFrame();
    for (Model m : models) {
      // If leaderboard frame exists, get metrics from there
      if (leaderboardFrame != null) {
        metrics[i++] = ModelMetrics.getMetricFromModelMetric(
            _leaderboard_model_metrics.get(ModelMetrics.buildKey(m, leaderboardFrame)),
            metric
        );
      } else {
        // otherwise use default model metrics
        Key model_key = m._key;
        long model_checksum = m.checksum();
        ModelMetrics mm = ModelMetrics.defaultModelMetrics(m);
        metrics[i++] = ModelMetrics.getMetricFromModelMetric(
            _leaderboard_model_metrics.get(ModelMetrics.buildKey(model_key, model_checksum, mm.frame()._key, mm.frame().checksum())),
            metric
        );
      }
    }
    return metrics;
  }

  /**
   * Delete object and its dependencies from DKV, including models.
   */
  @Override
  protected Futures remove_impl(Futures fs, boolean cascade) {
    Log.debug("Cleaning up leaderboard from models "+Arrays.toString(_model_keys));
    if (cascade) {
      for (Key<Model> m : _model_keys) {
        Keyed.remove(m, fs, true);
      }
    }
    for (Key k : _leaderboard_model_metrics.keySet())
      Keyed.remove(k, fs, true);
    return super.remove_impl(fs, cascade);
  }

  private static String[] defaultMetricsForModel(Model m) {
    if (m._output.isBinomialClassifier()) { //binomial
      return new String[] {"auc", "logloss", "aucpr", "mean_per_class_error", "rmse", "mse"};
    } else if (m._output.isMultinomialClassifier()) { // multinomial
      return new String[] {"mean_per_class_error", "logloss", "rmse", "mse"};
    } else if (m._output.isSupervised()) { // regression
      return new String[] {"mean_residual_deviance", "rmse", "mse", "mae", "rmsle"};
    }
    return new String[0];
  }

  private double[] getModelMetricValues(int rank) {
    assert rank >= 0 && rank < getModelKeys().length: "invalid rank";
    if (_metrics == null) return new double[0];
    final double[] values = new double[_metrics.length];
    for (int i=0; i < _metrics.length; i++) {
      values[i] = _metric_values.get(_metrics[i])[rank];
    }
    return values;
  }

  String rankTsv() {
    String lineSeparator = "\n";

    StringBuilder sb = new StringBuilder();
    sb.append("Error").append(lineSeparator);

    for (int i = getModelKeys().length - 1; i >= 0; i--) {
      // TODO: allow the metric to be passed in.  Note that this assumes the validation (or training) frame is the same.
      sb.append(Arrays.toString(getModelMetricValues(i)));
      sb.append(lineSeparator);
    }
    return sb.toString();
  }

  private TwoDimTable makeTwoDimTable(String tableHeader, int nrows, LeaderboardColumn... columns) {
    assert columns.length > 0;
    assert _sort_metric != null || nrows == 0 :
        "sort_metrics needs to be always not-null for non-empty array!";

    String description = nrows > 0 ? "models sorted in order of "+_sort_metric+", best first"
                        : "no models in this leaderboard";
    String[] rowHeaders = new String[nrows];
    for (int i = 0; i < nrows; i++) rowHeaders[i] = ""+i;
    String[] colHeaders = Stream.of(columns).map(LeaderboardColumn::getName).toArray(String[]::new);
    String[] colTypes = Stream.of(columns).map(LeaderboardColumn::getType).toArray(String[]::new);
    String[] colFormats = Stream.of(columns).map(LeaderboardColumn::getFormat).toArray(String[]::new);
    String colHeaderForRowHeader = nrows > 0 ? "#" : "-";
    return new TwoDimTable(
            tableHeader,
            description,
            rowHeaders,
            colHeaders,
            colTypes,
            colFormats,
            colHeaderForRowHeader
    );
  }

  private void addTwoDimTableRow(TwoDimTable table, int row, String modelID, String[] metrics, LeaderboardCell[] extensions) {
    int col = 0;
    table.set(row, col++, modelID);
    for (String metric : metrics) {
      double value = _metric_values.get(metric)[row];
      table.set(row, col++, value);
    }
    for (LeaderboardCell extension: extensions) {
      if (extension != null) {
        Object value = extension.getValue() == null ? extension.fetch() : extension.getValue(); // for costly extensions, only fetch value on-demand
        if (!extension.isNA()) {
          table.set(row, col, value);
        }
      }
      col++;
    }
  }

  /**
   * Creates a {@link TwoDimTable} representation of the leaderboard.
   * If no extensions are provided, then the representation will only contain the model ids and the scoring metrics.
   * Each extension name will be represented in the table
   * if and only if it was also made available to the leaderboard by the {@link LeaderboardExtensionsProvider},
   * otherwise it will just be ignored.
   * @param extensions optional columns for the leaderboard representation.
   * @return a {@link TwoDimTable} representation of the current leaderboard.
   * @see LeaderboardExtensionsProvider
   * @see LeaderboardColumn
   */
  public TwoDimTable toTwoDimTable(String... extensions) {
    return toTwoDimTable("Leaderboard for project " + _project_name, false, extensions);
  }

  private TwoDimTable toTwoDimTable(String tableHeader, boolean leftJustifyModelIds, String... extensions) {
    final Key<Model>[] modelKeys = _model_keys.clone(); // leaderboard can be retrieved when AutoML is still running: freezing current models state.
    final List<LeaderboardColumn> columns = new ArrayList<>();
    final List<LeaderboardColumn> extColumns = new ArrayList<>();
    String[] metrics = _metrics == null ? (_sort_metric == null ? new String[0] : new String[] {_sort_metric})
                      : _metrics;
    columns.add(ModelId.COLUMN);
    for (String metric: metrics) {
      columns.add(MetricScore.getColumn(metric));
    }
    if (getModelCount() > 0) {
      final Key<Model> leader = getModelKeys()[0];
      LeaderboardCell[] extCells = (extensions.length > 0 && LeaderboardExtensionsProvider.ALL.equalsIgnoreCase(extensions[0]))
              ? getExtensions(leader)
              : Stream.of(extensions).map(e -> getExtension(leader, e)).toArray(LeaderboardCell[]::new);
      Stream.of(extCells).filter(Objects::nonNull).forEach(e -> extColumns.add(e.getColumn()));
    }
    columns.addAll(extColumns);

    TwoDimTable table = makeTwoDimTable(tableHeader, modelKeys.length, columns.toArray(new LeaderboardColumn[0]));

    int maxModelIdLen = Stream.of(modelKeys).mapToInt(k -> k.toString().length()).max().orElse(0);
    final String[] modelIDsFormatted = new String[modelKeys.length];
    for (int i = 0; i < modelKeys.length; i++) {
      Key<Model> key = modelKeys[i];
      if (leftJustifyModelIds) {
        // %-s doesn't work in TwoDimTable.toString(), so fake it here:
        modelIDsFormatted[i] = org.apache.commons.lang.StringUtils.rightPad(key.toString(), maxModelIdLen);
      } else {
        modelIDsFormatted[i] = key.toString();
      }
      addTwoDimTableRow(table, i,
              modelIDsFormatted[i],
              metrics,
              extColumns.stream().map(ext -> getExtension(key, ext.getName())).toArray(LeaderboardCell[]::new)
      );
    }
    return table;
  }

  private String toString(String fieldSeparator, String lineSeparator, boolean includeTitle, boolean includeHeader) {
    final StringBuilder sb = new StringBuilder();
    if (includeTitle) {
      sb.append("Leaderboard for project \"")
              .append(_project_name)
              .append("\": ");

      if (_model_keys.length == 0) {
        sb.append("<empty>");
        return sb.toString();
      }
      sb.append(lineSeparator);
    }

    boolean printedHeader = false;
    for (int i = 0; i < _model_keys.length; i++) {
      final Key<Model> key = _model_keys[i];
      if (includeHeader && ! printedHeader) {
        sb.append("model_id");
        sb.append(fieldSeparator);
        String [] metrics = _metrics != null ? _metrics : new String[0];
        sb.append(Arrays.toString(metrics));
        sb.append(lineSeparator);
        printedHeader = true;
      }

      sb.append(key.toString());
      sb.append(fieldSeparator);
      double[] values = _metrics != null ? getModelMetricValues(i) : new double[0];
      sb.append(Arrays.toString(values));
      sb.append(lineSeparator);
    }
    return sb.toString();
  }

  @Override
  public String toString() {
    return toString(" ; ", " | ", true, true);
  }

  public String toLogString() {
    return toTwoDimTable("Leaderboard for project "+_project_name, true).toString();
  }

}
