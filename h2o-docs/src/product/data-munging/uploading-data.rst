上传文件
----------------

与并行读取器的导入函数不同，上传函数是从客户机推送到服务器，指定的路径必须是客户端路径。这是不可伸缩的，只适用于较小的数据集。客户机将数据从本地文件系统（例如，在运行R或Python的机器上）推送到AIR。对于大数据操作，您不希望数据存储在客户机上或流经客户机。

参考 `支持的文件格式 <http://docs.h2o.ai/h2o/latest-stable/h2o-docs/getting-data-into-h2o.html#supported-file-formats>`__ 主题以确保使用受支持的文件类型。

**注意**： 当解析包含不包含时区的时间戳的数据文件时，时间戳将被解析为为UTC (GMT)时间。您可以使用以下命令覆盖解析时区：

  - R: ``h2o.setTimezone("America/Los Angeles")``
  - Python: ``h2o.cluster().timezone = "America/Los Angeles"``

运行以下命令来加载驻留在运行AIR的同一台机器上的数据。

.. tabs::
   .. code-tab:: r R
	
		library(h2o)
		h2o.init()
		irisPath <- "../smalldata/iris/iris_wheader.csv"
		iris.hex <- h2o.uploadFile(path = irisPath, destination_frame = "iris.hex")
	  
   .. code-tab:: python
   
		import h2o
		h2o.init()
		iris_df = h2o.upload_file("../smalldata/iris/iris_wheader.csv")
