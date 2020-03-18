均值编码
---------------

均值编码是用目标变量的均值替换分类值的过程。任何非分类列都会被目标编码器模型自动删除。在本例中，我们将尝试使用清理后的贷款俱乐部数据预测 ``bad_loan``： https://raw.githubusercontent.com/h2oai/app-consumer-loan/master/data/loan.csv.

其中一个预测因子是 ``addr_state``，包含50个惟一值的分类列。在 ``addr_state`` 上执行均值编码，将计算每一个州的 ``bad_loan`` 平均值（由于 ``bad_loan`` 是二项的，这将转化为 ``bad_loan = 1`` 占记录的比例）

例如，``addr_state`` 的均值编码可以是：

+---------------+---------------------+
| addr\_state   | average bad\_loan   |
+===============+=====================+
| AK            | 0.1476998           |
+---------------+---------------------+
| AL            | 0.2091603           |
+---------------+---------------------+
| AR            | 0.1920290           |
+---------------+---------------------+
| AZ            | 0.1740675           |
+---------------+---------------------+
| CA            | 0.1780015           |
+---------------+---------------------+
| CO            | 0.1433022           |
+---------------+---------------------+

我们可以使用状态的均值编码，而不是在模型中使用状态作为预测因子。

在本主题中，我们将介绍使用均值编码将分类列转换为数字列的步骤。这有助于提高机器学习的准确性，因为算法往往很难处理高基数列。

jupyter notebook， `categorical predictors with tree based model <https://github.com/h2oai/h2o-tutorials/blob/master/best-practices/categorical-predictors/gbm_drf.ipynb>`__, 讨论处理高基数列的两种方法：

-  比较删除高基数列后的模型性能
-  参数调整（特别是调整 ``nbins_cats`` 和 ``categorical_encoding`` 参数

在本主题中，我们将尝试使用均值编码来改进模型性能。

训练基线模型
~~~~~~~~~~~~~~~~~~~~

下面的例子展示了如何训练基线模型。

.. tabs::
   .. code-tab:: r R

        library(h2o)
        h2o.init()

        # Start by training a model using the original data. 
        # Below we import our data into the H2O cluster.
        df <- h2o.importFile("https://raw.githubusercontent.com/h2oai/app-consumer-loan/master/data/loan.csv")
        df$bad_loan <- as.factor(df$bad_loan)

        # Randomly split the data into 75% training and 25% testing. 
        # We will use the testing data to evaluate how well the model performs.
        splits <- h2o.splitFrame(df, seed = 1234, 
                                 destination_frames=c("train.hex", "test.hex"), 
                                 ratios = 0.75)
        train <- splits[[1]]
        test <- splits[[2]]

        # Now train the baseline model. 
        # We will train a GBM model with early stopping.
        response <- "bad_loan"
        predictors <- c("loan_amnt", "int_rate", "emp_length", "annual_inc", "dti", 
                        "delinq_2yrs", "revol_util", "total_acc", "longest_credit_length",
                        "verification_status", "term", "purpose", "home_ownership", 
                        "addr_state")

        gbm_baseline <- h2o.gbm(x = predictors, y = response, 
                                training_frame = train, validation_frame = test,
                                score_tree_interval = 10, ntrees = 500,
                                sample_rate = 0.8, col_sample_rate = 0.8, seed = 1234,
                                stopping_rounds = 5, stopping_metric = "AUC", 
                                stopping_tolerance = 0.001,
                                model_id = "gbm_baseline.hex")

        # Get the AUC on the training and testing data:
        train_auc <- h2o.auc(gbm_baseline, train = TRUE)
        valid_auc <- h2o.auc(gbm_baseline, valid = TRUE)

        auc_comparison <- data.frame('Data' = c("Training", "Validation"),
                                     'AUC' = c(train_auc, valid_auc))

        auc_comparison
                Data       AUC
        1   Training 0.7492599
        2 Validation 0.7070187


   .. code-tab:: python

        import h2o
        h2o.init()

        # Start by training a model using the original data. 
        # Below we import our data into the H2O cluster.
        df = h2o.import_file("https://raw.githubusercontent.com/h2oai/app-consumer-loan/master/data/loan.csv")
        df['bad_loan'] = df['bad_loan'].asfactor()

        # Randomly split the data into 75% training and 25% testing. 
        # We will use the testing data to evaluate how well the model performs.
        train, test = df.split_frame(ratios=[0.75], seed=1234)

        # Now train the baseline model. 
        # We will train a GBM model with early stopping.
        from h2o.estimators.gbm import H2OGradientBoostingEstimator
        predictors = ["loan_amnt", "int_rate", "emp_length", "annual_inc", "dti", 
                      "delinq_2yrs", "revol_util", "total_acc", "longest_credit_length",
                      "verification_status", "term", "purpose", "home_ownership", 
                      "addr_state"]
        response = "bad_loan"

        gbm_baseline=H2OGradientBoostingEstimator(score_tree_interval=10,
                                                  ntrees=500,
                                                  sample_rate=0.8,
                                                  col_sample_rate=0.8,
                                                  seed=1234,
                                                  stopping_rounds=5,
                                                  stopping_metric="AUC",
                                                  stopping_tolerance=0.001,
                                                  model_id="gbm_baseline.hex")

        gbm_baseline.train(x=predictors, y=response, training_frame=train,
                           validation_frame=test)

        # Get the AUC on the training and testing data:
        train_auc = gbm_baseline.auc(train=True)
        train_auc
        0.7492599314713426

        valid_auc = gbm_baseline.auc(valid=True)
        valid_auc
        0.707018686126265


我们的训练数据比我们的验证数据有更高的AUC。查看变量重要性值，以查看最重要的变量。

.. tabs::
   .. code-tab:: r R

        # Variable Importance
        h2o.varimp_plot(gbm_baseline)

   .. code-tab:: python

        # Variable Importance
        gbm_baseline.varimp_plot()

.. figure:: ../images/gbm_variable_importance1.png
   :alt: GBM Variable importance - first run
   :height: 348
   :width: 325

最重要的变量是 ``int_rate`` ，``addr_state`` ，``annual_inc`` 和 ``term``。 ``int_rate`` 具有如此高的变量重要性是有道理的，因为它与贷款违约有关，但令人惊讶的是， ``addr_state`` 有如此高的变量重要性。高变量重要性可能是因为我们的模型通过这个高基数分类列来记忆训练数据。

如果我们移除 ``addr_state`` 预测因子，看看AUC在测试数据上是否提升。这说明模型正在记忆训练数据。

.. tabs::
   .. code-tab:: r R


        predictors <- setdiff(predictors, "addr_state")

        gbm_no_state <- h2o.gbm(x = predictors, y = response, 
                                training_frame = train, validation_frame = test, 
                                score_tree_interval = 10, ntrees = 500,
                                sample_rate = 0.8, col_sample_rate = 0.8, seed = 1234,
                                stopping_rounds = 5, stopping_metric = "AUC", stopping_tolerance = 0.001,
                                model_id = "gbm_no_state.hex")

        # Get the AUC for the baseline model and the model without ``addr_state``
        auc_baseline <- h2o.auc(gbm_baseline, valid = TRUE)
        auc_nostate <- h2o.auc(gbm_no_state, valid = TRUE)

        auc_comparison <- data.frame('Model' = c("Baseline", "No addr_state"),
                                     'AUC' = c(auc_baseline, auc_nostate))

        auc_comparison
                  Model       AUC
        1      Baseline 0.7070187
        2 No addr_state 0.7076197

   .. code-tab:: python

        predictors = ["loan_amnt", "int_rate", "emp_length", "annual_inc", "dti",
                      "delinq_2yrs", "revol_util", "total_acc", "longest_credit_length",
                      "verification_status", "term", "purpose", "home_ownership"]

        gbm_no_state=H2OGradientBoostingEstimator(score_tree_interval=10,
                                                  ntrees=500,
                                                  sample_rate=0.8,
                                                  col_sample_rate=0.8,
                                                  seed=1234,
                                                  stopping_rounds=5,
                                                  stopping_metric="AUC",
                                                  stopping_tolerance=0.001,
                                                  model_id="gbm_no_state.hex")

        gbm_no_state.train(x=predictors, y=response, training_frame=train,
                           validation_frame=test)

        auc_baseline = gbm_baseline.auc(valid=True)
        auc_baseline
        0.707018686126265

        auc_nostate = gbm_no_state.auc(valid=True)
        auc_nostate
        0.7076197256885596

如果我们不包括 ``addr_state`` 状态预测因子，我们的测试AUC会有轻微的改善。这是一个很好的迹象，GBM模型可能与这一列过度拟合。

在AIR-3中进行均值编码
~~~~~~~~~~~~~~~~~~~~~~~~

现在，我们将在 ``addr_state`` 上执行均值编码，看看这种表示形式是否提高了模型性能。

AIR-3中的均值编码分两步执行：

1. 使用 ``target_encode_fit`` 创建（拟合）均值编码映射。这将包含响应列和计数的和。这可以包括一个可选的 ``fold_column`` 。

2. 使用 ``target_encode_transform`` 转化均值编码映射。通过添加带有均值编码值的新列，将均值编码映射应用于数据。

执行均值编码时可以使用以下选项，其中一些选项可以防止过拟合：

-  ``holdout_type``
-  ``blended_avg``
-  ``noise``
-  ``fold_column``
-  ``smoothing``
-  ``inflection_point``
-  ``seed``

Holdout Type
''''''''''''

``holdout_type`` 参数定义是否应该在所有数据行上构造目标平均值。在计算训练数据的目标平均时，可以通过去除一些不匹配数据来防止过拟合。

可以指定以下holdout类型：

-  ``none``: 均值对所有数据行进行计算 \*\* 。 这应该用于测试数据
-  ``loo``: 均值是对除行本身之外的所有数据行进行计算。

   - 这可以用于训练数据集。行本身的目标不包括在平均值中，以防止过拟合。

-  ``kfold``: 平均值仅在非折叠数据上计算。（此选项需要折叠列。）

   -  这可以用于训练数据集。 为了防止过拟合，目标均值是根据叠外数据计算的。

Blended Average
'''''''''''''''

``blended_avg`` 参数定义是否应基于组的计数对目标平均值进行加权。通常情况下，一些组可能只有少量记录，目标平均值将不可靠。 为了防止这种情况发生，混合平均值取组目标值和全局目标值的加权平均值。

Noise
'''''

如果要将随机噪声添加到目标平均值中，可以使用 ``noise`` 参数指定要添加的噪声量。该值默认为0.01 \* 随机噪声的y范围。

Fold Column
'''''''''''

指定数据中折叠列的名称或列索引。该值默认为NULL (没有 ``fold_column``).

Smoothing
'''''''''

平滑值用于混合和计算 ``lambda``。平滑控制特定水平的后验概率与先验概率之间的转换速率。对于接近无穷大的平滑值，它成为后验概率和先验概率之间的一个硬阈值。该值默认为20。

Inflection Point
''''''''''''''''

拐点值用于混合和计算 ``lambda``。这决定了最小样本量的一半，我们完全相信在分类变量的特定级别上基于样本量的估计。该值默认为10。

Seed
''''

指定一个随机种子，用于从随机噪声的均匀分布中生成绘图。该值默认为-1。


执行均值编码
~~~~~~~~~~~~~~~~~~~~~~~

首先拟合均值编码映射。这含有每个州的不良贷款数据（``numerator``）和每个州的行数（``denominator``）。 拟合均值编码映射后，对每个州应用(转换)均值编码。

拟合均值编码映射
'''''''''''''''''''''''''''

.. tabs::
   .. code-tab:: r R

        # Create a fold column in the train dataset
        train$fold <- h2o.kfold_column(train, nfolds=5, seed = 1234)

        # Fit the target encoding map
        te_map <- h2o.target_encode_fit(train, x = list("addr_state"), 
                                        y = response, fold_column = "fold")

   .. code-tab:: python

        # Create a fold column in the train dataset
        fold = train.kfold_column(n_folds=5, seed=1234)
        fold.set_names(["fold"])
        train = train.cbind(fold)

        # Set the predictor to be "addr_state"
        predictor = ["addr_state"]

        # Fit the target encoding map
        from h2o.targetencoder import TargetEncoder
        target_encoder = TargetEncoder(x=predictor, y=response, 
                                       fold_column="fold", 
                                       blended_avg= True, 
                                       inflection_point = 3, 
                                       smoothing = 1, 
                                       seed=1234)
        target_encoder.fit(train)

转化均值编码
'''''''''''''''''''''''''

将均值编码应用到我们的训练和测试数据中。

**将均值编码应用于训练数据集** 

.. tabs::
   .. code-tab:: r R

        # Transform the target encoding on the training dataset
        encoded_train <- h2o.target_encode_transform(train, x = list("addr_state"), y = response, 
                                                     target_encode_map = te_map, holdout_type = "kfold",
                                                     fold_column="fold", blended_avg = TRUE, 
                                                     inflection_point=3, smoothing=1, seed = 1234,
                                                     noise=0.2)

   .. code-tab:: python
    
        # noise = 0.2 will be applied
        encoded_train = target_encoder.transform(frame=train, holdout_type="kfold", noise=0.2, seed=1234)

**将均值编码应用于测试数据集**

我们不需要应用任何过拟合预防技术，因为我们的均值编码映射是在训练数据上创建的，而不是在测试数据上。

-  ``holdout_type="none"``
-  ``blended_avg=FALSE``
-  ``noise=0`` 

.. tabs::
   .. code-tab:: r R

        encoded_test <- h2o.target_encode_transform(test, x = list("addr_state"), y = response,
                                                    target_encode_map = te_map, holdout_type = "none",
                                                    fold_column = "fold", noise = 0,
                                                    blended_avg = FALSE, seed=1234)

   .. code-tab:: python
   
        target_encoder_test = TargetEncoder(x=predictor, y=response, blended_avg=False)
        target_encoder_test.fit(train)
        
        # Applying encoding map that was generated on `train` data to the `test`. 
        encoded_test = target_encoder_test.transform(frame=test, holdout_type="none", noise=0.0, seed=1234)


用K折均值编码训练模型
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

训练一个新模型，这次用 ``addr_state_te`` 替换 ``addr_state`` 。

.. tabs::
   .. code-tab:: r R

        predictors <- c("loan_amnt", "int_rate", "emp_length", "annual_inc", 
                        "dti", "delinq_2yrs", "revol_util", "total_acc", 
                        "longest_credit_length", "verification_status", "term", 
                        "purpose", "home_ownership", "addr_state_te")

        gbm_state_te <- h2o.gbm(x = predictors, 
                                y = response, 
                                training_frame = encoded_train, 
                                validation_frame = encoded_test, 
                                score_tree_interval = 10, 
                                ntrees = 500,
                                stopping_rounds = 5, 
                                stopping_metric = "AUC", 
                                stopping_tolerance = 0.001,
                                model_id = "gbm_state_te.hex",
                                seed=1234)

   .. code-tab:: python

        predictors = ["loan_amnt", "int_rate", "emp_length", "annual_inc", 
                      "dti", "delinq_2yrs", "revol_util", "total_acc", 
                      "longest_credit_length", "verification_status", "term", 
                      "purpose", "home_ownership", "addr_state_te"]

        gbm_state_te = H2OGradientBoostingEstimator(score_tree_interval = 10, 
                                ntrees = 500,
                                stopping_rounds = 5, 
                                stopping_metric = "AUC", 
                                stopping_tolerance = 0.001,
                                model_id = "gbm_state_te.hex",
                                seed=1234)
        gbm_state_te.train(x=predictors, y=response, 
                           training_frame=encoded_train, 
                           validation_frame=encoded_test)

三个模型的AUC如下所示：

.. tabs::
   .. code-tab:: r R

        # Get AUC
        auc_state_te <- h2o.auc(gbm_state_te, valid = TRUE)

        auc_comparison <- data.frame('Model' = c("No Target Encoding", 
                                                 "No addr_state", 
                                                 "addr_state Target Encoding"),
                                     'AUC' = c(auc_baseline, auc_nostate, auc_state_te))

        auc_comparison
                               Model       AUC
        1         No Target Encoding 0.7070187
        2              No addr_state 0.7076197
        3 addr_state Target Encoding 0.7072750

   .. code-tab:: python

        # Compare AUC values:

        valid_auc = gbm_baseline.auc(valid=True)
        valid_auc
        0.707018686126265

        auc_nostate = gbm_no_state.auc(valid=True)
        auc_nostate
        0.7076197256885596

        auc_state_te = gbm_state_te.auc(valid=True)
        auc_state_te
        0.7072749724799465

现在 ``addr_state_te`` 的变量重要性要小得多，它不再是第二重要的因素，而是第十重要的因素。  

.. tabs::
   .. code-tab:: r R

        # Variable Importance
        h2o.varimp_plot(gbm_state_te)

   .. code-tab:: python

        # Variable Importance
        gbm_state_te.varimp_plot()

.. figure:: ../images/gbm_variable_importance2.png
   :alt: GBM Variable importance - second run
   :scale: 75%

参考
~~~~~~~~~~

-  `Target Encoding in H2O-3 Demo <https://github.com/h2oai/h2o-3/blob/master/h2o-r/demos/rdemo.target_encode.R>`__
-  `Automatic Feature Engineering Webinar <https://www.youtube.com/watch?v=VMTKcT1iHww>`__
-   Daniele Micci-Barreca. 2001. A preprocessing scheme for high-cardinality categorical attributes in classification and prediction problems. SIGKDD Explor. Newsl. 3, 1 (July 2001), 27-32.
-  `Zumel, Nina B. and John Mount. "vtreat: a data.frame Processor for Predictive Modeling." (2016). <https://arxiv.org/abs/1611.09477>`__
