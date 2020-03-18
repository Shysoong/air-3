导入多个文件
------------------------

 ``importFolder`` (R)/``import_file`` (Python) 函数可以通过指定目录和模式来导入多个本地文件。示例模式包括：

- ``pattern="/A/.*/iris_.*"``: 导入指定目录下符合 ``/A/.*/iris_.*`` 模式的所有文件。
- ``pattern="/A/iris_.*"``: 导入指定目录下符合 ``/A/iris_.*`` 模式的所有文件。
- ``pattern="/A/B/iris_.*"``: 导入指定目录下符合 ``/A/B/iris_.*`` 模式的所有文件。
- ``pattern="iris_.*"``: 导入指定目录下符合 ``iris_.*`` 模式的所有文件。

**注意**: 

- 指定要包含的所有文件必须具有相同的列数和列集。
- 当解析包含不包含时区的时间戳的数据文件时，时间戳将被解析为为UTC (GMT)时间。您可以使用以下命令覆盖解析时区：

  - R: ``h2o.setTimezone("America/Los Angeles")``
  - Python: ``h2o.cluster().timezone = "America/Los Angeles"``

- 下面的示例假设已经克隆了AIR-3 GitHub存储库，并在 **air-3** 文件夹中运行以下命令来抓取 **smalldata** 数据集。

  :: 

    ./gradlew syncSmalldata


.. tabs::
   .. code-tab:: r R
	
		# To import all .csv files from the prostate_folder directory:
		library(h2o)
		h2o.init()
		prosPath <- system.file("extdata", "prostate_folder", package = "h2o")
		prostate_pattern.hex <- h2o.importFolder(path = prosPath, 
		                                         pattern = ".*.csv", 
		                                         destination_frame = "prostate.hex")
		class(prostate_pattern.hex)
		summary(prostate_pattern.hex)

		# To import all .csv files from an anomaly folder stored locally
		ecgPath <- "../path_to_h2o-3/smalldata/anomaly/"
		ecg_pattern.hex <- h2o.importFolder(path=ecgPath, 
		                                    pattern = ".*.csv", 
		                                    destination_frame = "ecg_pattern.hex")

		class(ecg_pattern.hex)
		summary(ecg_pattern.hex)
	  
   .. code-tab:: python

		# To import all .csv files from an anomaly folder stored locally matching the regex ".*\.csv"
		import h2o
		h2o.init()
		ecg_pattern = h2o.import_file(path="../path_to_h2o-3/smalldata/anomaly/",pattern = ".*\.csv")

