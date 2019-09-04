数据填充
-------------

impute函数允许您通过使用 "na.rm’d" 向量上计算的聚合填充缺失的值来执行原位计算。此外，还可以根据数据集中列的分组执行计算。这些列可以通过索引或列名传递给``by``参数。注意，如果提供了因子列，那么方法必须是 ``mode`` 。

 ``impute`` 函数接受以下参数：

- ``dataset``: 包含要填充的列的数据集
- ``column``: 指定要填充的列。默认值 ``0`` 指定填充整个帧。
- ``method``: 要执行的填充类型。``mean`` 用列平均值替换NA值，``median`` 用列中位数替换NA值; ``mode`` 用最常见的因子替换（只适用于因子列）。
- ``combine_method``: 如果方法是 ``median``，然后选择如何组合均匀样本大小的分位数，在所有其他情况下都忽略此参数。 ``combine_method`` 可用选项包括插值、平均值、低值和高值。
- ``by``: 分组列
- ``groupByFrame`` 或 ``group_by_frame``: 用这个预先计算好的分组帧填充列。
- ``values``:  输入值的向量（每列一个）。NaN代表跳过该列。

.. example-code::
   .. code-block:: r

	library(h2o)
	h2o.init()

   	# Upload the Airlines dataset
   	filePath <- "https://s3.amazonaws.com/h2o-airlines-unpacked/allyears2k.csv"
   	air <- h2o.importFile(filePath, "air")
   	print(dim(air))
   	43978    31

   	# Show the number of rows with NA.
   	print(numNAs <- sum(is.na(air$DepTime)))
   	[1] 1086

   	DepTime_mean <- mean(air$DepTime, na.rm = TRUE)
   	print(DepTime_mean)
   	[1] 1345.847

   	# Mean impute the DepTime column
   	h2o.impute(air, "DepTime", method = "mean")
   	 [1]     NaN      NaN      NaN      NaN 1345.847      NaN      NaN      NaN
	 [9]     NaN      NaN      NaN      NaN      NaN      NaN      NaN      NaN
	[17]     NaN      NaN      NaN      NaN      NaN      NaN      NaN      NaN
	[25]     NaN      NaN      NaN      NaN      NaN      NaN      NaN

	# Revert the imputations
	air <- h2o.importFile(filePath, "air")

	# Impute the column using a grouping based on the Origin and Distance
	# If the Origin and Distance produce groupings of NAs, then no imputation will be done (NAs will result).
	h2o.impute(air, "DepTime", method = "mean", by = c("Dest"))
	  Dest mean_DepTime
	1  ABE     1671.795
	2  ABQ     1308.074
	3  ACY     1651.095
	4  ALB     1405.412
	5  AMA     1404.333
	6  ANC     2022.000

	[134 rows x 2 columns]

	# Revert the imputations
	air <- h2o.importFile(filePath, "air")

	# Impute a factor column by the most common factor in that column
	h2o.impute(air, "TailNum", method = "mode")
	 [1]  NaN  NaN  NaN  NaN  NaN  NaN  NaN  NaN  NaN  NaN 3499  NaN  NaN  NaN  NaN
	[16]  NaN  NaN  NaN  NaN  NaN  NaN  NaN  NaN  NaN  NaN  NaN  NaN  NaN  NaN  NaN
	[31]  NaN

	# Revert imputations
	air <- h2o.importFile(filePath, "air")

	# Impute a factor column using a grouping based on the Month
	h2o.impute(air, "TailNum", method = "mode", by=c("Month"))
	  Month mode_TailNum
	1     1         3499
	2    10         3499

   .. code-block:: python

    import h2o
    h2o.init()

    # Import the airlines dataset
    air_path = "https://s3.amazonaws.com/h2o-airlines-unpacked/allyears2k.csv"
    air = h2o.import_file(path=air_path)
    air.dim
    [43978, 31]

    # Mean impute the DepTime column based on the Origin and Distance columns
    DeptTime_impute = air.impute("DepTime", method = "mean", by = ["Origin", "Distance"])
    DeptTime_impute
    Origin      Distance    mean_DepTime
    --------  ----------  --------------
    ABE              253         1149.7
    ABE              481          812
    ABQ              223         1229.33
    ABQ              277         1565
    ABQ              289         1529
    ABQ              321         1267.06
    ABQ              328         1301.85
    ABQ              332         1655
    ABQ              349          813.28
    ABQ              487         1536.14

    [1497 rows x 3 columns]

    # Revert imputations
    air = h2o.import_file(path=air_path)

    # Mode impute the TailNum column
    mode_impute = air.impute("TailNum", method = "mode")
    mode_impute
    [nan, nan, nan, nan, nan, nan, nan, nan, nan, nan, 3499.0, nan, nan, nan, nan, nan, nan, nan, nan, nan, nan, nan, nan, nan, nan, nan, nan, nan, nan, nan, nan]

    # Revert imputations
    air = h2o.import_file(path=air_path)

    # Mode impute the TailNum column based on the Month and Year columns
    mode_impute = air.impute("TailNum", method = "mode", by=["Month", "Year"])
    mode_impute
    Year    Month    mode_TailNum
    ------  -------  --------------
      1987       10            3499
      1988        1            3499
      1989        1            3499
      1990        1            3499
      1991        1            3499
      1992        1            3499
      1993        1            3499
      1994        1            3499
      1995        1            3500
      1996        1             672

    [22 rows x 3 columns]

