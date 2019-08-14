extensions = dict(
    extra_params=[('verbose', 'FALSE')],
    skip_default_set_params_for=['training_frame', 'ignored_columns', 'response_column', 'max_confusion_matrix_size',
                                 'distribution', 'offset_column'],
    set_params="""
if (!missing(distribution)) {
  warning("Argument distribution is deprecated and has no use for Random Forest.")
  parms$distribution <- 'AUTO'
}
if (!missing(offset_column)) {
  warning("Argument offset_column is deprecated and has no use for Random Forest.")
  parms$offset_column <- NULL
}
"""
)


doc = dict(
    preamble="""
Build a Random Forest model

Builds a Random Forest model on an H2OFrame.
""",
    params=dict(
        distribution="Distribution. This argument is deprecated and has no use for Random Forest.",
        offset_column="Offset column. This argument is deprecated and has no use for Random Forest.",
        verbose="""
\code{Logical}. Print scoring history to the console (Metrics per tree). Defaults to FALSE.
"""
    ),
    returns="""
Creates a \linkS4class{H2OModel} object of the right type.
""",
    seealso="""
\code{\link{predict.H2OModel}} for prediction
""",
)
