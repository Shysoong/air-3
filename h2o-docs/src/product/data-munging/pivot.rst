透视数据表
---------------

使用这个函数来透视表。这是通过指定三个列来执行的：索引、列和值。 Index is the column where pivoted rows should be aligned on; column represents the column to pivot; and value specifies the values of the pivoted table. For cases with multiple indexes for a column label, the aggregation method is to pick the first occurrence in the data frame.

**注意**: 

 - 单个索引值的所有行必须装载于一个节点。

 - 单个索引值和列标签的最大行数为 ``Chunk size * Chunk size``.

.. tabs::
   .. code-tab:: r R

        library(h2o)
        h2o.init()

        # Create a simple data frame by inputting values
        data <- data.frame(colorID = c('1','2','3','3','1','4'), 
                           value = c('red','orange','yellow','yellow','red','blue'), 
                           amount = c('4','2','4','3','6','3'))
        df <- as.h2o(data)
        
        # View the dataset
        df
          colorID  value amount
        1       1    red      4
        2       2 orange      2
        3       3 yellow      4
        4       3 yellow      3
        5       1    red      6
        6       4   blue      3

        [6 rows x 3 columns]

        # Pivot the table on the colorID column and aligned on the amount column
        df2 <- h2o.pivot(df,index="amount",column="colorID",value="value")
        df2
          amount   1   2   3   4
        1      2 NaN   1 NaN NaN
        2      3 NaN NaN   3   0
        3      4   2 NaN   3 NaN
        4      6   2 NaN NaN NaN

        [4 rows x 5 columns] 

   .. code-tab:: python

        import h2o
        h2o.init()

        # Create a simple data frame by inputting values
        df = h2o.H2OFrame({'colorID': ['1','2','3','3','1','4'],
                           'value': ['red','orange','yellow','yellow','red','blue'],
                           'amount': ['4','2','4','3','6','3']})

        # View the dataset
        df
        colorID      amount  value
        ---------  --------  -------
                1         4  red
                2         2  orange
                3         4  yellow
                3         3  yellow
                1         6  red
                4         3  blue

        [6 rows x 3 columns]

        # Pivot the table on the colorID column and aligned on the amount column
        df2 = df.pivot(index="amount",column="colorID",value="value")
        df2
          amount    1    2    3    4
        --------  ---  ---  ---  ---
               2  nan    1  nan  nan
               3  nan  nan    3    0
               4    2  nan    3  nan
               6    2  nan  nan  nan

        [4 rows x 5 columns]
