.. _save_and_load_model:

保存和加载模型
==========================

This section describes how to save and load binary and :ref:`MOJO models <about-pojo-mojo>` using R, Python, and Flow. 

Binary Models
-------------

When saving an H2O binary model with ``h2o.saveModel`` (R), ``h2o.save_model`` (Python), or in Flow, you will only be able to load and use that saved binary model with the same version of H2O that you used to train your model. H2O binary models are not compatible across H2O versions. If you update your H2O version, then you will need to retrain your model. For production, you can save your model as a :ref:`POJO/MOJO <about-pojo-mojo>`. These artifacts are not tied to a particular version of H2O because they are just plain Java code and do not require an H2O cluster to be running.

In R and Python
~~~~~~~~~~~~~~~

In R and Python, you can save a model locally or to HDFS using the ``h2o.saveModel`` (R) or ``h2o.save_model`` (Python) function . This function accepts the model object and the file path. If no path is specified, then the model will be saved to the current working directory. After the model is saved, you can load it using the ``h2o.loadModel`` (R) or ``h2o.load_model`` (Python) function.

.. example-code::
   .. code-block:: r

    # build the model
    model <- h2o.deeplearning(params)

    # save the model
    model_path <- h2o.saveModel(object=model, path=getwd(), force=TRUE)

    print(model_path)
    /tmp/mymodel/DeepLearning_model_R_1441838096933

    # load the model
    saved_model <- h2o.loadModel(model_path)

   .. code-block:: python

	# build the model
	model = H2ODeepLearningEstimator(params)
	model.train(params)

	# save the model
	model_path = h2o.save_model(model=model, path="/tmp/mymodel", force=True)

	print model_path
	/tmp/mymodel/DeepLearning_model_python_1441838096933

	# load the model
	saved_model = h2o.load_model(model_path)
 

**Note**: When saving to HDFS, you must prepend the save directory with ``hdfs://``. For example:

.. example-code::
   .. code-block:: r

    # build the model
    model <- h2o.glm(model params)

    # save the model to HDFS
    hdfs_name_node <- "node-1"
    hdfs_tmp_dir <- "/tmp/runit"
    model_path <- sprintf("hdfs://%s%s", hdfs_name_node, hdfs_tmp_dir)
    h2o.saveModel(model, dir=model_path, name="mymodel")

   .. code-block:: python

	# build the model
	h2o_glm = H2OGeneralizedLinearEstimator(model params)
	h2o_glm.train(training params)

	# save the model to HDFS
	hdfs_name_node = "node-1"
	hdfs_model_path = sprintf("hdfs://%s%s", hdfs_name_node, hdfs_tmp_dir)
	new_model_path = h2o.save_model(h2o_glm, "hdfs://" + hdfs_name_node + "/" + hdfs_model_path)

In Flow
~~~~~~~

The steps for saving and loading models in Flow are described in the **Using Flow - H2O's Web UI** section. Specifically, refer to :ref:`Exporting and Importing Models <export-import-models-flow>` for information about loading models into Flow. 

MOJO Models
-----------

Introduction
~~~~~~~~~~~~

The MOJO import functionality provides a means to use external, pre-trained models in H2O - mainly for the purpose of scoring. Depending on each external model, metrics and other model information might be obtained as well. Currently, only selected H2O MOJOs are supported. (See the :ref:`mojo_quickstart` section for information about creating MOJOs.)

Supported MOJOs
~~~~~~~~~~~~~~~

Only a subset of H2O MOJO models is supported in this version. 

-  GBM (Gradient Boosting Machines)
-  DRF (Distributed Random Forest)
-  IRF (Isolation Random Forest)
-  GLM (Generalized Linear Model)

Importing a MOJO
~~~~~~~~~~~~~~~~~~~~~~~~~

Importing a MOJO can be done from Python, R, and Flow. H2O imports the model and embraces it for the purpose of scoring. Information output about the model may be limited.

Importing in R or Python
''''''''''''''''''''''''

.. example-code::
   .. code-block:: r

    data <- h2o.importFile(path = 'training_dataset.csv')
    cols <- c("Some column", "Another column")
    original_model <- h2o.glm(x=cols, y = "response", training_frame = data)    

    path <- "/path/to/model/directory"
    mojo_destination <- h2o.download_mojo(model = original_model, path = path)
    imported_model <- h2o.import_mojo(mojo_destination)

    new_observations <- h2o.importFile(path = 'new_observations.csv')
    h2o.predict(imported_model, new_observations)

   .. code-block:: python

    data = h2o.import_file(path='training_dataset.csv')
    original_model = H2OGeneralizedLinearEstimator()
    original_model.train(x = ["Some column", "Another column"], y = "response", training_frame=data)

    path = '/path/to/model/directory/model.zip'
    original_model.download_mojo(path)

    imported_model = h2o.import_mojo(path)
    new_observations = h2o.import_file(path='new_observations.csv')
    predictions = imported_model.predict(new_observations)

Importing a MOJO Model in Flow
''''''''''''''''''''''''''''''

To import a MOJO model in Flow:

1. Import or upload the MOJO as a Generic model into H2O. To do this, click on **Data** in the top menu and select either **Import Files** or **Upload File**.
2. Retrieve the imported MOJO by clicking **Models** in the top menu and selecting **Import MOJO Model**.

Advanced MOJO Model Initialization
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

It is also possible to import a MOJO from already uploaded MOJO bytes using Generic model. Generic model is the underlying mechanism behind MOJO import. In this case, there is no need to re-upload the MOJO every time a new MOJO imported model is created. The upload can occur only once.

Defining a Generic Model
''''''''''''''''''''''''

The following options can be specified when using a Generic model:

- `model_id <algo-params/model_id.html>`__: Specify a custom name for the model to use as a reference.

- **model_key**: Specify a key for the self-contained model archive.

- **path**: Specify a path to the file with the self-contained model archive.

Examples
''''''''

.. example-code::
   .. code-block:: r

    data <- h2o.importFile(path = 'training_dataset.csv')
    cols <- c("Some column", "Another column")
    original_model <- h2o.glm(x=cols, y = "response", training_frame = data)    

    path <- "/path/to/model/directory"
    mojo_destination <- h2o.download_mojo(model = original_model, path = path)
    
    # Only import or upload MOJO model data, do not initialize the generic model yet
    imported_mojo_key <- h2o.importFile(mojo_destination, parse = FALSE)
    # Build the generic model later, when needed 
    generic_model <- h2o.generic(model_key = imported_mojo_key)

    new_observations <- h2o.importFile(path = 'new_observations.csv')
    h2o.predict(generic_model, new_observations)

   .. code-block:: python

    data = h2o.import_file(path='training_dataset.csv')
    original_model = H2OGeneralizedLinearEstimator()
    original_model.train(x = ["Some column", "Another column"], y = "response", training_frame=data)

    path = '/path/to/model/directory/model.zip'
    original_model.download_mojo(path)
    
    imported_mojo_key = h2o.lazy_import(file)
    generic_model = H2OGenericEstimator(model_key = get_frame(model_key[0]))
    new_observations = h2o.import_file(path='new_observations.csv')
    predictions = generic_model.predict(new_observations)

