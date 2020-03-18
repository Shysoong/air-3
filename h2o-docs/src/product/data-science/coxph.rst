比例风险回归模型(CoxPH)
--------------------------------

**注意** Python版本中还不支持CoxPH，它只在R和Flow版本中支持。

Cox比例风险模型是对事件的发生时间建模最广泛使用的方法。顾名思义， *hazard function* ,它计算事件发生的瞬时概率，并以数学形式 

:math:`h(t) = \lim_{\Delta t \downarrow 0} \frac{Pr[t \le T < t + \Delta t \mid T \ge t]}{\Delta t},`

表示它被认为是 *baseline hazard function* 和 *risk score* 的乘积。因此, 在比例风险回归模型中，对于观察变量 :math:`i` 的风险函数被定义为

:math:`h_i(t) = \lambda(t)\exp(\mathbf{x}_i^T\beta)`

:math:`\lambda(t)` 是所有观测值共享的一个与时间有关的基准危险率， :math:`\exp(\mathbf{x}_i^T\beta)` 是观察值 :math:`i` 的风险评分，这是通过使用了对所有观察值通用的系数变量  :math:`\beta` 的协变量向量  :math:`\mathbf{x}_i^T` 计算出来的。

这种结合非参数基准危险率和参数风险评分的Cox比例危险模型被描述为 *半参数* 。此外，模型名称也表明，与广义线性模型不同，风险评分中的截距(常数)项没有为模型的拟合增加任何值，这是由于包含了基准危险率。

`R展示程序可在这里找到 <https://github.com/h2oai/h2o-3/blob/master/h2o-r/demos/rdemo.coxph.R>`__。它在WA\_Fn-UseC\_-Telco-Customer-Churn.csv数据集上使用CoxPH算法。 

定义CoxPH模型
~~~~~~~~~~~~~~~~~~~~~~

-  `model_id <algo-params/model_id.html>`__: (可选) 指定要用作引用的模型的自定义名称。默认情况下，AIR自动生成一个目标键。

-  `training_frame <algo-params/training_frame.html>`__: (必须) 指定用于构建模型的数据集。 **注意**：在Flow环境下， 如果您从 ``Parse`` 单元格单击 **构建模型** 按钮，训练数据帧会自动载入。

-  `start_column <algo-params/start_column.html>`__: (可选) **源** 数据集中表示开始时间的整数列的名称。如果提供，则 **start_column** （开始列）的值必须严格小于每行中的 **stop_column** （停止列）。

-  `stop_column <algo-params/stop_column.html>`__: (必须) **源** 数据集中表示结束时间的整数列的名称。

-  `y <algo-params/y.html>`__: (必须) 指定要用作因变量的列。数据可以是数字的，也可以是分类的。

-  `ignored_columns <algo-params/ignored_columns.html>`__: (可选，Python和Flow独占) 指定要从模型中排除的列。在Flow中，单击列名旁边的复选框，将其添加到模型中排除的列列表中。要添加所有列，单击 **所有** 按钮。要从忽略列的列表中删除列，请单击列名旁边的X。要从忽略列列表中移除所有列，单击 **全不选** 按钮。要查找一个特定的列，在列列表上面的 **查找** 字段输入列名。 要只展示指定缺失值比例的列，在 **只展示缺失超过0%数据的列** 字段指定比例。更改隐藏列的选择，使用 **选择可见** 或 **取消选择可见** 按钮。

-  `weights_column <algo-params/weights_column.html>`__: 指定要用于观察权重的列，该列用于偏差校正。指定的 ``weights_column`` 必须被包含在指定的 ``training_frame`` 内。
   
    *Python独占*: 若要在向 ``x`` 传递AIRFrame而不是列名列表时使用权重列，指定的 ``training_frame`` 必须包含指定的 ``weights_column``。
   
    **注意**: 权重是每行观察值的权重，不会增加数据帧的大小。这通常是一行重复的次数，但也支持非整数值。在训练过程中，由于损失函数的预因子较大，权重较高的行影响更大。

-  `offset_column <algo-params/offset_column.html>`__: 指定要用作偏移量的列。
   
	 **注意**: 偏移量是模型训练中使用的每行 "偏差值" 。对于高斯分布，它们可以看作是对响应(y)列的简单修正。模型不是学习预测响应(y-row)，而是学习预测响应列的(行)偏移量。对于其他分布，在应用反向链接函数得到实际响应值之前，先在线性化的空间中应用偏移校正。有关更多信息，请参考以下内容 `链接 <http://www.idg.pl/mirrors/CRAN/web/packages/gbm/vignettes/gbm.pdf>`__. 

-  **stratify_by**: 用于分层的列的列表。

-  `ties <algo-params/ties.html>`__: 处理部分似然关系的近似方法。可以是 **efron** (默认) 或者 **breslow**。 更多关于这些选项的信息请查看下边的 :ref:`coxph_model_details` 一节。

-  **init**: (可选) 模型中系数的初值。这个值默认为0。

-  **lre_min**: 一个正数，用作后续对数部分似然计算的最小对数相对误差(LRE)，以确定算法的收敛性。这个参数在模型拟合算法的停止条件中所起的作用在 :ref:`coxph_algorithm` 一节中进行来说明。该值默认为9。

-  `max_iterations <algo-params/max_iterations.html>`__: 定义模型训练期间最大迭代次数的正整数。这个参数在模型拟合算法的停止条件中所起的作用在 :ref:`coxph_algorithm` 一节中进行来说明。该值默认为20。

-  `interactions <algo-params/interactions.html>`__: 指定要交互的预测因子列索引列表。所有成对的组合将为该列表作计算。

-  `interaction_pairs <algo-params/interaction_pairs.html>`__: (与interactions联合使用) 定义interactions时，使用此选项指定成对交互的列表（两个变量之间的相互作用）。注意该选项和 ``interactions`` 不同，它将计算指定列的所有成对组合。 该选项默认不开启。

-  `export_checkpoints_dir <algo-params/export_checkpoints_dir.html>`__: 指定将自动导出生成模型的目录。

CoxPH模型结果
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

数据
''''''''

- Number of Complete Cases: 在任何输入列中没有缺失值的观察值的数量。
- Number of Non Complete Cases: 在任何输入列中至少缺少一个值的观察值的数目。
- Number of Events in Complete Cases: 在全部案例中观察到的事件数。

系数
''''''''''''

:math:`\tt{name}`: 系数的名字。如果预测因子列是数值的，则对应的系数具有相同的名称。如果预测因子列是分类的，对应的系数是列的名称与系数表示的类别级别的名称的串联。

:math:`\tt{coef}`: 估计系数值。

:math:`\tt{exp(coef)}`: 指数系数值估计。

:math:`\tt{se(coef)}`: 系数估计的标准误差。

:math:`\tt{z}`: z统计量，即系数估计值与其标准误差之比。

模型统计
''''''''''''''''

-  Cox and Snell Generalized :math:`R^2`

  :math:`\tt{R^2} := 1 - \exp\bigg(\frac{2\big(pl(\beta^{(0)}) - pl(\hat{\beta})\big)}{n}\bigg)`

-  Maximum Possible Value for Cox and Snell Generalized :math:`R^2`

  :math:`\tt{Max. R^2} := 1 - \exp\big(\frac{2 pl(\beta^{(0)})}{n}\big)`

- 似然比检验

  :math:`2\big(pl(\hat{\beta}) - pl(\beta^{(0)})\big)`, which under the null
  hypothesis of :math:`\hat{beta} = \beta^{(0)}` follows a chi-square
  distribution with :math:`p` degrees of freedom.

沃尔德检验

  :math:`\big(\hat{\beta} - \beta^{(0)}\big)^T I\big(\hat{\beta}\big) \big(\hat{\beta} - \beta^{(0)}\big)`,
  which under the null hypothesis of :math:`\hat{beta} = \beta^{(0)}` follows a
  chi-square distribution with :math:`p` degrees of freedom. When there is a
  single coefficient in the model, the Wald test statistic value is that
  coefficient's z statistic.

Score (Log-Rank)校验

  :math:`U\big(\beta^{(0)}\big)^T \hat{I}\big(\beta^{0}\big)^{-1} U\big(\beta^{(0)}\big)`,
  which under the null hypothesis of :math:`\hat{beta} = \beta^{(0)}` follows a
  chi-square distribution with :math:`p` degrees of freedom.

 where

  :math:`n` is the number of complete cases

  :math:`p` is the number of estimated coefficients

  :math:`pl(\beta)` is the log partial likelihood

  :math:`U(\beta)` is the derivative of the log partial likelihood

  :math:`H(\beta)` is the second derivative of the log partial likelihood

  :math:`I(\beta) = - H(\beta)` is the observed information matrix


.. _coxph_model_details:

CoxPH模型详情
~~~~~~~~~~~~~~~~~~~~~~~~~~~

A Cox proportional hazards model measures time on a scale defined by the ranking of the :math:`M` distinct observed event occurrence times, :math:`t_1 < t_2 < \dots < t_M`. When no two events occur at the same time, the partial likelihood for the observations is given by

:math:`PL(\beta) = \prod_{m=1}^M\frac{\exp(w_m\mathbf{x}_m^T\beta)}{\sum_{j \in R_m} w_j \exp(\mathbf{x}_j^T\beta)}`

where :math:`R_m` is the set of all observations at risk of an event at time :math:`t_m`. In practical terms, :math:`R_m` contains all the rows where (if supplied) the start time is less than :math:`t_m` and the stop time is greater than or equal to :math:`t_m`. When two or more events are observed at the same time, the exact partial likelihood is given by

:math:`PL(\beta) = \prod_{m=1}^M\frac{\exp(\sum_{j \in D_m} w_j\mathbf{x}_j^T\beta)}{(\sum_{R^* : \mid R^* \mid = d_m} [\sum_{j \in R^*} w_j \exp(\mathbf{x}_j^T\beta)])^{\sum_{j \in D_m} w_j}}`

where :math:`R_m` is the risk set and :math:`D_m` is the set of observations of size :math:`d_m` with an observed event at time :math:`t_m` respectively. Due to the combinatorial nature of the denominator, this exact partial likelihood becomes prohibitively expensive to calculate, leading to the common use of Efron's and Breslow's approximations.

Efron's Approximation
'''''''''''''''''''''

Of the two approximations, Efron's produces results closer to the exact combinatoric solution than Breslow's. Under this approximation, the partial likelihood and log partial likelihood are defined as

:math:`PL(\beta) = \prod_{m=1}^M \frac{\exp(\sum_{j \in D_m} w_j\mathbf{x}_j^T\beta)}{\big[\prod_{k=1}^{d_m}(\sum_{j \in R_m} w_j \exp(\mathbf{x}_j^T\beta) - \frac{k-1}{d_m} \sum_{j \in D_m} w_j \exp(\mathbf{x}_j^T\beta))\big]^{(\sum_{j \in D_m} w_j)/d_m}}`

:math:`pl(\beta) = \sum_{m=1}^M \big[\sum_{j \in D_m} w_j\mathbf{x}_j^T\beta - \frac{\sum_{j \in D_m} w_j}{d_m} \sum_{k=1}^{d_m} \log(\sum_{j \in R_m} w_j \exp(\mathbf{x}_j^T\beta) - \frac{k-1}{d_m} \sum_{j \in D_m} w_j \exp(\mathbf{x}_j^T\beta))\big]`

Breslow's Approximation
'''''''''''''''''''''''

Under Breslow's approximation, the partial likelihood and log partial likelihood are defined as

:math:`PL(\beta) = \prod_{m=1}^M \frac{\exp(\sum_{j \in D_m} w_j\mathbf{x}_j^T\beta)}{(\sum_{j \in R_m} w_j \exp(\mathbf{x}_j^T\beta))^{\sum_{j \in D_m} w_j}}`

:math:`pl(\beta) = \sum_{m=1}^M \big[\sum_{j \in D_m} w_j\mathbf{x}_j^T\beta - (\sum_{j \in D_m} w_j)\log(\sum_{j \in R_m} w_j \exp(\mathbf{x}_j^T\beta))\big]`

.. _coxph_algorithm:

CoxPH模型算法
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

AIR使用Newton-Raphson算法来最大化局部对数似然，这是一个由步骤定义的迭代过程：

To add numeric stability to the model fitting calculations, the numeric predictors and offsets are demeaned during the model fitting process.

1. Set an initial value, :math:`\beta^{(0)}`, for the coefficient vector and assume an initial log partial likelihood of :math:`- \infty`.
2. Increment iteration counter, :math:`n`, by 1.
3. Calculate the log partial likelihood, :math:`pl\big(\beta^{(n)}\big)`, at the current coefficient vector estimate.
4. Compare :math:`pl\big(\beta^{(n)}\big)` to :math:`pl\big(\beta^{(n-1)}\big)`.

  a) If :math:`pl\big(\beta^{(n)}\big) > pl\big(\beta^{(n-1)}\big)`, then accept the new coefficient vector, :math:`\beta^{(n)}`, as the current best estimate, :math:`\tilde{\beta}`, and set a new candidate coefficient vector to be :math:`\beta^{(n+1)} = \beta^{(n)} - \tt{step}`, where :math:`\tt{step} := H^{-1}(\beta^{(n)}) U(\beta^{(n)})`, which is the product of the inverse of the second derivative of :math:`pl` times the first derivative of :math:`pl` based upon the observed data.

  b) If :math:`pl\big(\beta^{(n)}\big) \le pl\big(\beta^{(n-1)}\big)`, then set :math:`\tt{step} := \tt{step} / 2` and :math:`\beta^{(n+1)} = \tilde{\beta} - \tt{step}`.

5. Repeat steps 2 - 4 until either
  
  a) :math:`n = \tt{iter\ max}` or
  
  b) the log-relative error :math:`LRE\Big(pl\big(\beta^{(n)}\big), pl\big(\beta^{(n+1)}\big)\Big) >= \tt{lre\ min}`,
     
     where
     
     :math:`LRE(x, y) = - \log_{10}\big(\frac{\mid x - y \mid}{y}\big)`, if :math:`y \ne 0`

     :math:`LRE(x, y) = - \log_{10}(\mid x \mid)`, if :math:`y = 0`


参考
~~~~~~~~~~

Andersen, P. and Gill, R. (1982). Cox's regression model for counting processes, a large sample study. *Annals of Statistics* **10**, 1100-1120.

Harrell, Jr. F.E., Regression Modeling Strategies: With Applications to Linear Models, Logistic Regression, and Survival Analysis. Springer-Verlag, 2001.

Therneau, T., Grambsch, P., Modeling Survival Data: Extending the Cox Model. Springer-Verlag, 2000.
