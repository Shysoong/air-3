导入文件
----------------

与 `上传 <uploading-data.html>`__ 函数不同，它是一个从客户机到服务器的推送。导入函数是一个并行的读取器，从客户机指定的位置从服务器获取信息，该路径是服务器端路径。这是一种快速、可伸缩、高度优化的数据读取方式。AIR从数据存储中提取数据，并作为读取操作启动数据传输。

参考 `支持的文件格式 <http://docs.h2o.ai/h2o/latest-stable/h2o-docs/getting-data-into-h2o.html#supported-file-formats>`__ 主题以确保使用受支持的文件类型。

**注意**： 当解析包含不包含时区的时间戳的数据文件时，时间戳将被解析为为UTC (GMT)时间。您可以使用以下命令覆盖解析时区：

  - R: ``h2o.setTimezone("America/Los Angeles")``
  - Python: ``h2o.cluster().timezone = "America/Los Angeles"``

.. example-code::
   .. code-block:: r
	
	# To import airlines file from H2O’s package:
	library(h2o)
	h2o.init()
	irisPath <- "https://s3.amazonaws.com/h2o-airlines-unpacked/allyears2k.csv" 
	iris.hex <- h2o.importFile(path = irisPath, destination_frame = "iris.hex")
	  
	# To import from S3:
	library(h2o)
	h2o.init()
	airlinesURL <- "https://s3.amazonaws.com/h2o-airlines-unpacked/allyears2k.csv" 
	airlines.hex <- h2o.importFile(path = airlinesURL, destination_frame = "airlines.hex")

	# To import from HDFS, you must include the node name:
	library(h2o)
	h2o.init()
	airlinesURL <- "hdfs://node-1:/user/smalldata/airlines/allyears2k_headers.zip" 
	airlines.hex <- h2o.importFile(path = airlinesURL, destination_frame = "airlines.hex")
	  
   .. code-block:: python

	# Import a file from S3:
	import h2o
	h2o.init()
	airlines = "http://s3.amazonaws.com/h2o-public-test-data/smalldata/airlines/allyears2k_headers.zip"
	airlines_df = h2o.import_file(path=airlines)

	# Import a file from HDFS, you must include the node name:
	import h2o
	h2o.init()
	airlines = "hdfs://node-1:/user/smalldata/airlines/allyears2k_headers.zip"
	airlines_df = h2o.import_file(path=airlines)

