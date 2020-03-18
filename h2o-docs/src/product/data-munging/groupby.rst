分组
--------

``group_by`` 函数允许您对一个或多个列进行分组，并对结果应用一个函数。具体来说 ``group_by`` 函数在AIR数据帧上执行以下操作：

- 根据一些标准将数据分成组
- 将一个函数独立地应用于每个组
- 将结果合并到AIR数据帧中

结果是一个新的IR数据帧，列数与创建的组数相等。返回的组按自然顺序按列排序。

``group_by`` 函数接受以下参数：

**Python和R都可用**

 - H2O Frame: 这指定要对group by操作执行的AIR数据帧。
 - ``by``: ``by`` 选项可以获取列列表，如果您希望按多个列进行分组以计算摘要。 

**Python独占**

- ``na``, 在计算过程中控制NA值的处理。它可以是其中之一：

  - ``all`` (默认): 计算中按原样使用任何NA值，这通常会导致最终结果也是NA。
  - ``ignore``: NA项不包括在计算中，但是总项数作为总行数。例如， ``mean([1, 2, 3, nan], na="ignore")`` 结果是 ``1.5`` 。
  - ``rm``: 在计算期间跳过条目，从而减少了条目的有效总数。例如， ``mean([1, 2, 3, nan], na="rm")`` 结果是 ``2`` 。

**R独占**

 - ``gb.control``: 在R中，``gb.control`` 选项指定如何处理数据集中的NA值以及如何命名输出列。注意，要在``gb.control``列表中指定列名列表，必须增加 ``col.names`` 参数。
 - ``nrow``: 指定生成列的名称。
 - ``na.methods``, 在计算过程中控制NA值的处理。它可以是其中之一：

  - ``all`` (默认): 计算中按原样使用任何NA值，这通常会导致最终结果也是NA。
  - ``ignore``: NA项不包括在计算中，但是总项数作为总行数。例如， ``mean([1, 2, 3, nan], na="ignore")`` 结果是 ``1.5``。
  - ``rm``: 在计算期间跳过条目，从而减少了条目的有效总数。例如， ``mean([1, 2, 3, nan], na="rm")`` 结果是 ``2`` 。

  **注意**: 如果提供的列表小于列组的数量，则列表将由``ignore``填充。 

除了上述参数外，任何数量的以下聚合都可以通过 ``group_by`` 函数链接到组中: 

- ``count``: 计算GroupBy对象的每个组中的行数。
- ``max``: 计算GroupBy对象的每个组在 ``col`` 中指定的每个列的最大值。
- ``mean``: 计算GroupBy对象的每组在 ``col`` 中指定的每一列的平均值。
- ``min``: 计算GroupBy对象的每个组在 ``col`` 中指定的每个列的最小值。
- ``mode``: 计算GroupBy对象的每个组在 ``col`` 中指定的每个列的模式。
- ``sd``: 计算GroupBy对象的每组在 ``col`` 中指定的每一列的标准差。
- ``ss``: 计算GroupBy对象的每组在 ``col`` 中指定的每一列的平方和。
- ``sum``: 计算GroupBy对象的每组在 ``col`` 中指定的每一列的和。 
- ``var``: 计算GroupBy对象的每组在 ``col`` 中指定的每一列的方差。

 如果没有为聚合提供参数（例如， ``grouped.sum(col="X1", na="all").mean(col="X5", na="all").max()`` 中的 ``max()``），这时应该认为聚合函数应用于除GroupBy列之外的所有列。  

注意，一旦聚合操作完成，使用一组新的聚合调用GroupBy对象将不会产生任何效果。必须生成一个新的GroupBy对象，以便对其应用新的聚合。此外，某些聚合只定义为数值列或分类列。在错误的数据类型上调用聚合将引发错误。

.. tabs::
   .. code-tab:: r R

        library(h2o)
        h2o.init()

        # Import the airlines data set and display a summary.
        airlinesURL <- "https://s3.amazonaws.com/h2o-airlines-unpacked/allyears2k.csv"
        airlines.hex <- h2o.importFile(path = airlinesURL, destination_frame = "airlines.hex")
        summary(airlines.hex)

        # Find number of flights by airport
        originFlights <- h2o.group_by(data = airlines.hex, by = "Origin", nrow("Origin"), gb.control=list(na.methods="rm"))
        originFlights.R <- as.data.frame(originFlights)
        originFlights.R
            Origin nrow
        1      ABE   59
        2      ABQ  876
        3      ACY   31
        ...

        # Find number of flights per month
        flightsByMonth <- h2o.group_by(data = airlines.hex, 
                                       by = "Month", 
                                       nrow("Month"), 
                                       gb.control=list(na.methods="rm"))
        flightsByMonth.R <- as.data.frame(flightsByMonth)
        flightsByMonth.R
          Month   nrow
        1     1  41979
        2    10   1999

        # Find the number of flights in a given month based on the origin
        cols <- c("Origin","Month")
        flightsByOriginMonth <- h2o.group_by(data=airlines.hex, 
                                             by=cols, 
                                             nrow("Month"), 
                                             gb.control=list(na.methods="rm"))
        flightsByOriginMonth.R <- as.data.frame(flightsByOriginMonth)
        flightsByOriginMonth.R
            Origin Month nrow
        1      ABE     1   59
        2      ABQ     1  846
        3      ABQ    10   30
        4      ACY     1   31
        5      ALB     1   75
        ...

        # Find months with the highest cancellation ratio
        which(colnames(airlines.hex)=="Cancelled")
        [1] 22
        cancellationsByMonth <- h2o.group_by(data = airlines.hex, 
                                             by = "Month", 
                                             sum("Cancelled"), 
                                             gb.control=list(na.methods="rm"))
        cancellation_rate <- cancellationsByMonth$sum_Cancelled/flightsByMonth$nrow
        rates_table <- h2o.cbind(flightsByMonth$Month,cancellation_rate)
        rates_table.R <- as.data.frame(rates_table)
        rates_table.R
          Month sum_Cancelled
        1     1   0.025417471
        2    10   0.009504752

        # Use group_by with multiple columns. Summarize the destination, 
        # arrival delays, and departure delays for an origin
        cols <- c("Dest", "IsArrDelayed", "IsDepDelayed")
        originFlights <- h2o.group_by(data = airlines.hex[c("Origin",cols)], 
                                      by = "Origin", 
                                      sum(cols),
                                      gb.control = list(na.methods = "ignore", col.names = NULL))
        
        # Note a warning because col.names null
        res <- h2o.cbind(lapply(cols, function(x){h2o.group_by(airlines.hex,by="Origin",sum(x))}))[,c(1,2,4,6)]
        res
          Origin sum_Dest sum_IsArrDelayed sum_IsDepDelayed
        1    ABE     5884               40               30
        2    ABQ    84505              545              370
        3    ACY     3131                9                7
        4    ALB     3646               49               50
        5    AMA      317                4                6
        6    ANC      100                0                1

   .. code-tab:: python

        import h2o
        h2o.init()

        # Upload the airlines dataset
        air = h2o.import_file("https://s3.amazonaws.com/h2o-airlines-unpacked/allyears2k.csv")
        air.dim
        [43978, 31]

        # Find number of flights by airport
        originFlights = air.group_by("Origin")
        originFlights.count()
        originFlights.get_frame()
        Origin      nrow
        --------  ------
        ABE           59
        ABQ          876
        ACY           31
        ...

        # Find number of flights per month based on the origin
        cols = ["Origin","Month"]
        flights_by_origin_month = air.group_by(by=cols).count(na ="all")
        flights_by_origin_month.get_frame()
        Origin      Month    nrow
        --------  -------  ------
        ABE             1      59
        ABQ             1     846
        ABQ            10      30
        ...

        # Find months with the highest cancellation ratio
        cancellation_by_month = air.group_by(by='Month').sum('Cancelled', na ="all")
        flights_by_month = air.group_by('Month').count(na ="all")
        cancelled = cancellation_by_month.get_frame()['sum_Cancelled']
        flights = flights_by_month.get_frame()['nrow']
        month_count = flights_by_month.get_frame()['Month']
        ratio = cancelled/flights
        month_count.cbind(ratio)
          Month    sum_Cancelled
          -------  ---------------
                1       0.0254175
               10       0.00950475

        [2 rows x 2 columns]

        # Use group_by with multiple columns. Summarize the destination, 
        # arrival delays, and departure delays for an origin
        cols_1 = ['Origin', 'Dest', 'IsArrDelayed', 'IsDepDelayed']
        cols_2 = ["Dest", "IsArrDelayed", "IsDepDelayed"]
        air[cols_1].group_by(by='Origin').sum(cols_2, na ="ignore").get_frame()
        Origin      sum_Dest    sum_IsDepDelayed    sum_IsArrDelayed
        --------  ----------  ------------------  ------------------
        ABE             5884                  30                  40
        ABQ            84505                 370                 545
        ACY             3131                   7                   9
        ALB             3646                  50                  49
        AMA              317                   6                   4
        ANC              100                   1                   0
        ...
