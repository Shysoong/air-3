.. _pojo_quickstart:

POJO快速入门
----------------

本节描述如何构建和实现一个POJO来使用预测评分。Java开发人员应该参考该 `Javadoc <http://docs.h2o.ai/h2o/latest-stable/h2o-genmodel/javadoc/index.html>`__ 获得更多信息。

**注意**: 

- 对于大于1G的源文件，不支持POJOs。有关更多信息，请参考下面的POJO FAQ部分。POJOs也不支持XGBoost、GLRM或Stacked Ensembles模型。 
- POJO预测无法解析双引号括起来的列（例如，""2""）。

什么是POJO?
~~~~~~~~~~~~~~~

AIR允许您将构建的模型转换为简单的旧Java对象(POJO)，然后可以轻松地将其部署到Java应用程序中，并计划在指定的数据集上运行。

POJO允许用户使用AIR构建模型，然后使用POJO模型或对计分服务器的REST API调用部署模型来实时打分。

生成模型的惟一编译和运行时依赖项是 ``h2o-genmodel.jar`` 。作为这些包的构建输出生成的jar文件。这个文件是一个支持计分的库，它包含派生POJO的基类（您可以在pojo类中看到 "extends GenModel" 。GenModel类是这个库的一部分）。当POJO模型部署到生产环境中时，``h2o-genmodel.jar`` 是必须的。 

构建POJO
~~~~~~~~~~~~~~~

下面的示例代码展示了如何启动AIR流程、编译和运行示例流程，然后编译和运行POJO。本例使用GBM，但是任何受支持的算法都可以用来构建模型并运行POJO。 

1. 在终端窗口1启动AIR：

	``$ java -jar h2o.jar``

2. 使用您的网页浏览器构建模型：

   a. 访问 http://localhost:54321
   b. 在靠近窗口右上角单击 **查看示例流程** 。下面是你要找的东西的截屏：

    .. figure:: /howto/images/view_example_flows.png
      :alt: View Example Flows
      :width: 272
      :height: 237

   c. 单击 ``GBM_Airlines_Classification.flow``

   d. 如果出现一个确认提示，要求您“加载笔记本”，单击它。

   e. 从"流程笔记"菜单中选择"运行所有单元格"选项

   f. 向下滚动，在笔记本中找到"Model"单元格。如下面的截图所示，单击 **下载 POJO** 按钮：

    .. figure:: /howto/images/download_pojo.png
       :alt: Download POJO

    **注意**： 下面的说明假设POJO模型已下载到"Downloads"文件夹。

3. 在 *新* 终端窗口中下载模型片段。注意，AIR必须仍然在终端窗口1中运行：

   ::

       $ mkdir experiment
       $ cd experiment
       $ mv ~/Downloads/gbm_pojo_test.java .
       $ curl http://localhost:54321/3/h2o-genmodel.jar > h2o-genmodel.jar

4. 通过使用以下内容创建一个名为main.java (``vim main.java``)的新文件，在终端窗口2中创建主程序：

   ::

       import java.io.*;
       import hex.genmodel.easy.RowData;
       import hex.genmodel.easy.EasyPredictModelWrapper;
       import hex.genmodel.easy.prediction.*;

       public class main {
         private static String modelClassName = "gbm_pojo_test";

         public static void main(String[] args) throws Exception {
           hex.genmodel.GenModel rawModel;
           rawModel = (hex.genmodel.GenModel) Class.forName(modelClassName).newInstance();
           EasyPredictModelWrapper model = new EasyPredictModelWrapper(rawModel);

           RowData row = new RowData();
           row.put("Year", "1987");
           row.put("Month", "10");
           row.put("DayofMonth", "14");
           row.put("DayOfWeek", "3");
           row.put("CRSDepTime", "730");
           row.put("UniqueCarrier", "PS");
           row.put("Origin", "SAN");
           row.put("Dest", "SFO");

           BinomialModelPrediction p = model.predictBinomial(row);
           System.out.println("Label (aka prediction) is flight departure delayed: " + p.label);
           System.out.print("Class probabilities: ");
           for (int i = 0; i < p.classProbabilities.length; i++) {
             if (i > 0) {
               System.out.print(",");
             }
             System.out.print(p.classProbabilities[i]);
           }
           System.out.println("");
         }
       }

5. 在终端窗口2中编译POJO：

   ::

       $ javac -cp h2o-genmodel.jar -J-Xmx2g -J-XX:MaxPermSize=128m gbm_pojo_test.java main.java

6. 在终端窗口2中运行POJO。

 对于Linux和OS X 用户： ``$ java -cp .:h2o-genmodel.jar main``

 对于Windows用户： ``$ java -cp .;h2o-genmodel.jar main``

 以下输出被打印：

 ::

     Label (aka prediction) is flight departure delayed: YES
     Class probabilities: 0.4319916897116479,0.5680083102883521

从AIR中提取模型
~~~~~~~~~~~~~~~~~~~~~~~~~~

生成的模型可以通过以下方法从AIR中提取：

从AIR FLOW网页用户界面
''''''''''''''''''''''''

当查看模型的时候，在模型单元格上方单击 **下载 POJO** 按钮，如快速启动部分中的示例所示。您还可以在Flow中预览POJO，但是它只会在web浏览器中显示前一千行左右，从而截断大型模型。

从R或者Python中提取
''''''''''''''''''''

下面的代码片段展示了用AIR构建模型并从R脚本和Python脚本下载相应POJO的示例。 

.. example-code::
   .. code-block:: r

	    library(h2o)
	    h2o.init()
	    path <- system.file("extdata", "prostate.csv", package = "h2o")
	    h2o_df <- h2o.importFile(path)
	    h2o_df$CAPSULE <- as.factor(h2o_df$CAPSULE)
	    model <- h2o.glm(y = "CAPSULE",
	                    x = c("AGE", "RACE", "PSA", "GLEASON"),
	                    training_frame = h2o_df,
	                    family = "binomial")
	    h2o.download_pojo(model)

   .. code-block:: python

	    import h2o
	    h2o.init()
	    from h2o.estimators.glm import H2OGeneralizedLinearEstimator
	    path = "http://s3.amazonaws.com/h2o-public-test-data/smalldata/prostate/prostate.csv.zip"
	    h2o_df = h2o.import_file(path)
	    h2o_df['CAPSULE'] = h2o_df['CAPSULE'].asfactor()
	    model = H2OGeneralizedLinearEstimator(family = "binomial")
	    model.train(y = "CAPSULE",
	                x = ["AGE", "RACE", "PSA", "GLEASON"],
	                training_frame = h2o_df)
	    h2o.download_pojo(model)

.. raw:: html

   <!---

   **From Java:**

   TODO: provide pointer of doing this directly from Java
   From Sparkling Water:
   TODO: provide pointer of doing this from Sparkling Water

   -->

用例
~~~~~~~~~

下面用代码示例演示了以下用例：

-  **从CSV文件中读取新数据并对其进行预测**： PredictCsv类被AIR测试工具用于预测新的数据点。
-  **从JSON请求中获取一个新的观察结果并返回一个预测**
-  **直接从hive调用用户定义的函数**: 查看 `AIR-3训练github库 <https://github.com/h2oai/h2o-world-2015-training/tree/master/tutorials/hive_udf_template>`__.

FAQ
~~~

-  **如何在生产环境中实时为新场景打分**

  如果您正在使用UI，请单击模型的 **预览 POJO** 按钮。这将生成一个Java类，其中包含可以在生产环境应用程序中引用和使用的方法。

-  **我需要使用什么样的技术呢？**

  在JVM中运行需要的任何东西。POJO是一个独立的Java类，不依赖于AIR。

-  **在调用POJO之前，我应该如何格式化数据？**

  以下是我们的需求（假设您正在为POJO使用`Javadoc <http://h2o-release.s3.amazonaws.com/h2o/%7B%7Bbranch_name%7D%7D/%7B%7Bbuild_number%7D%7D/docs-website/h2o-genmodel/javadoc/index.html>`__中描述的“简单”预测API）。

   -  输入列必须只包含训练期间看到的分类级别
   -  任何不用于训练的额外输入列都将被忽略
   -  如果没有指定输入列，则将其视为 ``NA``
   -  有些模型不能很好地处理NA数据 (例如,GLM)
   -  在模型训练之前应用于数据的任何转换都必须在调用POJO预测方法之前应用

-  **如何在Spark集群上运行POJO？**

  POJO只提供用于进行预测的数学逻辑，因此您不会在其中找到任何Spark(甚至AIR)特定的代码。如果希望使用POJO对Spark中的数据集进行预测，请创建一个Map来为每一行调用POJO，并将结果逐行保存到一个新的列中。

-  **如何使用REST API与远程集群通信？**

  您可以通过REST API下载POJO，但是当调用POJO的预测方法时，时在同一个JVM中，而不是REST API来调用的 

-  **是否可以使用AIR集群和REST API进行预测？**

  是的，但这种预测方法与POJO是分开的。有关在AIR中预测(与POJO预测相反)的更多信息，请参阅AIR REST API端点的文档 /3/Predictions。

-  **为什么我在尝试编译POJO时收到以下错误？**

  当源文件大于1G时，将生成以下错误。

  ::

      Michals-MBP:b michal$ javac -cp h2o-genmodel.jar -J-Xmx2g -J-XX:MaxPermSize=128m drf_b9b9d3be_cf5a_464a_b518_90701549c12a.java
      An exception has occurred in the compiler (1.7.0_60). Please file a bug at the Java Developer Connection (http://java.sun.com/webapps/bugreport)  after checking the Bug Parade for duplicates. Include your program and the following diagnostic in your report.  Thank you.
      java.lang.IllegalArgumentException
          at java.nio.ByteBuffer.allocate(ByteBuffer.java:330)
          at com.sun.tools.javac.util.BaseFileManager$ByteBufferCache.get(BaseFileManager.java:308)
          at com.sun.tools.javac.util.BaseFileManager.makeByteBuffer(BaseFileManager.java:280)
          at com.sun.tools.javac.file.RegularFileObject.getCharContent(RegularFileObject.java:112)
          at com.sun.tools.javac.file.RegularFileObject.getCharContent(RegularFileObject.java:52)
          at com.sun.tools.javac.main.JavaCompiler.readSource(JavaCompiler.java:571)
          at com.sun.tools.javac.main.JavaCompiler.parse(JavaCompiler.java:632)
          at com.sun.tools.javac.main.JavaCompiler.parseFiles(JavaCompiler.java:909)
          at com.sun.tools.javac.main.JavaCompiler.compile(JavaCompiler.java:824)
          at com.sun.tools.javac.main.Main.compile(Main.java:439)
          at com.sun.tools.javac.main.Main.compile(Main.java:353)
          at com.sun.tools.javac.main.Main.compile(Main.java:342)
          at com.sun.tools.javac.main.Main.compile(Main.java:333)
          at com.sun.tools.javac.Main.compile(Main.java:76)
          at com.sun.tools.javac.Main.main(Main.java:61)

