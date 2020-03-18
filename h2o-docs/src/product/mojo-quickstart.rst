.. _mojo_quickstart:

MOJO快速入门
----------------

本节描述如何构建和实现一个MOJO(经过优化的模型对象)来使用预测评分。Java开发人员应该参考该 `Javadoc <http://docs.h2o.ai/h2o/latest-stable/h2o-genmodel/javadoc/index.html>`__ 获得更多信息。

什么是MOJO?
~~~~~~~~~~~~~~~

MOJO(优化的模型对象)是AIR POJO的替代方案。与POJOs一样，H2O允许您将构建的模型转换为MOJOs，然后可以部署MOJOs进行实时评分。

**注意**: 

- MOJOs支持AutoML, Deep Learning, DRF, GBM, GLM, GLRM, K-Means, PCA, Stacked Ensembles, SVM, Word2vec和XGBoost模型。
- MOJOs只支持默认编码或``enum``编码。 
- MOJO预测不能解析双引号括起来的列（例如，""2""）。

MOJOs优于POJOs的优点
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

虽然仍然支持POJOs，但是一些客户遇到了大型POJOs不能编译的问题。（注意，对于大于1G的源文件，不支持POJO）MOJOs没有大小限制，它通过从POJO中把树抽取出来解决大小问题，并使用通用的树遍历代码来导航模型。生成的可执行文件比POJO小得多，速度也快得多。

在大规模上，与POJOs相比，新模型的磁盘空间大约小20-25倍，“热”（在JVM能够优化典型的执行路径之后）评分快2-3倍，“冷”（当JVM还不知道执行路径时）评分快10-40倍。模型尺寸越大，效率增益越大。

AIR使用深度为25的5000棵树的模型进行了内部测试，在非常小的规模（50棵树/深度为5），在二项式和回归模型中，POJOs的执行速度比MOJOs快大约10%，但是在多项式模型中慢了50%。

构建一个MOJO
~~~~~~~~~~~~~~~

MOJOs的构建方式与POJOs非常相似。下边的示例代码展示了何如启动AIR，使用R或Python构建模型，然后编译并运行MOJO。本例使用GBM，但是任何受支持的算法都可以用来构建模型并运行MOJO。

下面的示例描述了如何启动AIR并使用R和Python创建模型。 ``download_mojo()`` 函数的作用是将模型保存为一个zip文件。您可以解压缩文件，以查看用于构建文件的选项以及模型中构建的每棵树。注意，每个树文件都保存为二进制文件类型。

步骤1：构建并提取一个模型
'''''''''''''''''''''''''''''''''

.. tabs::
   .. code-tab:: r R

    # 1. 打开终端窗口并启动R。

    # 2. 运行以下命令构建一个简单的GBM模型。
        library(h2o)
        h2o.init(nthreads=-1)
        path <- system.file("extdata", "prostate.csv", package="h2o")
        h2o_df <- h2o.importFile(path)
        h2o_df$CAPSULE <- as.factor(h2o_df$CAPSULE)
        model <- h2o.gbm(y="CAPSULE",
                         x=c("AGE", "RACE", "PSA", "GLEASON"),
                         training_frame=h2o_df,
                         distribution="bernoulli",
                         ntrees=100,
                         max_depth=4,
                         learn_rate=0.1)

        # Download the MOJO and the resulting h2o-genmodel.jar file 
        # to a new **experiment** folder. Note that the ``h2o-genmodel.jar`` file 
        # is a library that supports scoring and contains the required readers 
        # and interpreters. This file is required when MOJO models are deployed 
        # to production. Be sure to specify the entire path, not just the relative path.
        modelfile <- h2o.download_mojo(model, path="~/experiments/", get_genmodel_jar=TRUE)
        print("Model saved to " + modelfile)
        Model saved to /Users/user/GBM_model_R_1475248925871_74.zip"

   .. code-block:: python

    # 1. 打开终端窗口并启动python。
    
    # 2. 运行以下命令构建一个简单的GBM模型。
        # The model, along with the **h2o-genmodel.jar** file will 
        # then be downloaded to an **experiment** folder.
        import h2o
        from h2o.estimators.gbm import H2OGradientBoostingEstimator
        h2o.init()
        h2o_df = h2o.load_dataset("prostate.csv")
        h2o_df["CAPSULE"] = h2o_df["CAPSULE"].asfactor()
        model=H2OGradientBoostingEstimator(distribution="bernoulli",
                                           ntrees=100,
                                           max_depth=4,
                                           learn_rate=0.1)
        model.train(y="CAPSULE",
                    x=["AGE","RACE","PSA","GLEASON"],
                    training_frame=h2o_df)

        # Download the MOJO and the resulting ``h2o-genmodel.jar`` file 
        # to a new **experiment** folder. Note that the ``h2o-genmodel.jar`` file 
        # is a library that supports scoring and contains the required readers 
        # and interpreters. This file is required when MOJO models are deployed 
        # to production. Be sure to specify the entire path, not just the relative path.
        modelfile = model.download_mojo(path="~/experiment/", get_genmodel_jar=True)
        print("Model saved to " + modelfile)
        Model saved to /Users/user/GBM_model_python_1475248925871_888.zip           


   .. code-tab:: java

        // Compile the source: 
        javac -classpath ~/h2o/h2o-3.20.0.1/h2o.jar src/h2oDirect/h2oDirect.java

        // Execute as a classfile. This also downloads the LoanStats4 demo,
        // which trains a GBM model.
        Erics-MBP-2:h2oDirect ericgudgion$ java -cp /Users/ericgudgion/NetBeansProjects/h2oDirect/src/:/Users/ericgudgion/h2o/h2o-3.20.0.1/h2o.jar h2oDirect.h2oDirect /Demos/Lending-Club/LoanStats4.csv 
        ...
        06-14 20:40:29.420 192.168.1.160:54321   55005  main      INFO: Found XGBoost backend with library: xgboost4j_minimal
        06-14 20:40:29.428 192.168.1.160:54321   55005  main      INFO: Your system supports only minimal version of XGBoost (no GPUs, no multithreading)!
        06-14 20:40:29.428 192.168.1.160:54321   55005  main      INFO: ----- H2O started  -----
        06-14 20:40:29.428 192.168.1.160:54321   55005  main      INFO: Build git branch: rel-wright
        ...
        ...
        Starting H2O with IP 192.168.1.160:54321
        Loading data from file 
        ...
        Loaded file /Demos/Lending-Club/LoanStats4.csv size 3986423 Cols:19 Rows:39029
        ...
        Creating GBM Model
        Training Model
        ...
        Training Results
        Model Metrics Type: Binomial
         Description: N/A
         model id: GBM_model_1529023227180_1
         frame id: dataset-key
         MSE: 0.11255783
         RMSE: 0.3354964
         AUC: 0.82892376
         logloss: 0.36827797
         mean_per_class_error: 0.26371866
         default threshold: 0.261136531829834
        ...
        Model AUC 0.8289237508508612
        Model written out as a mojo to file /Demos/Lending-Club/LoanStats4.csv.zip

        // Save as h2oDirect.java
        package h2oDirect;

        import hex.tree.gbm.GBM;
        import hex.tree.gbm.GBMModel;
        import hex.tree.gbm.GBMModel.GBMParameters;
        import java.io.FileOutputStream;
        import java.io.IOException;
        import java.net.InetAddress;
        import water.Key;
        import water.fvec.Frame;
        import water.fvec.NFSFileVec;
        import water.parser.ParseDataset;
        import water.*;


        public class h2oDirect {

            
            /**
             * @param args the command line arguments
             */
            public static void main(String[] args) throws IOException {

              String h2oargs = "-nthreads -1 ";
              H2OApp.main(h2oargs.split(" "));
              System.out.println("Starting H2O with IP "+H2O.getIpPortString());
            
              H2O.waitForCloudSize(1, 3000);  
                 
              System.out.println("Loading data from file ");
              String inputfile = args[0];
              NFSFileVec datafile = NFSFileVec.make(inputfile);
              Frame dataframe = ParseDataset.parse(Key.make("dataset-key") , datafile._key);
              System.out.println("Loaded file "+inputfile+" size "+datafile.byteSize()+" Cols:"+dataframe.numCols()+" Rows:"+dataframe.numRows());
              
              
              for (int v=0; v<dataframe.numCols(); v++) {
              System.out.println(dataframe.name(v)+" "+dataframe.vec(v).get_type_str());
              }
              
              int c = dataframe.find("bad_loan");
              
              dataframe.replace(c, dataframe.vec(c).toCategoricalVec());
              
              
              // drop the id and member_id columns from model
              dataframe.remove(dataframe.find("id"));
              dataframe.remove(dataframe.find("member_id"));
              
              System.out.println("Creating GBM Model");
              
              GBMParameters modelparms = new GBMParameters();
              modelparms._train = dataframe._key;
              modelparms._response_column = "bad_loan";
              
              System.out.println("Training Model");
              GBM model = new GBM(modelparms);
              GBMModel gbm = model.trainModel().get();
              
              System.out.println("Training Results");
              System.out.println(gbm._output);
              System.out.println("Model AUC "+gbm.auc());
              
              
              String outputfile = inputfile+".zip";
              FileOutputStream modeloutput = new FileOutputStream(outputfile);
              gbm.getMojo().writeTo(modeloutput);
              modeloutput.close();
              System.out.println("Model written out as a mojo to file "+outputfile);
              
              System.out.println("H2O shutdown....");
              H2O.shutdown(0);
             
            }
            
        }

   .. code-tab:: scala

        import water.rapids.ast.prims.advmath.AstCorrelation

        object RandomForestFileInput {
          
          import water.H2O
          import water.H2OApp
          import water.fvec.Vec
          import water.fvec.NFSFileVec
          import water.fvec._
          
          import hex.tree.drf.DRF
          import hex.tree.drf.DRFModel
          import hex.tree.drf.DRFModel.DRFParameters
          import water.parser.ParseDataset
          import water.Key
          import water.Futures
          import water._

          import scala.io.Source
          import scala.reflect._
          
          import java.io.FileOutputStream
          import java.io.FileWriter
          
             def main(args: Array[String]): Unit = {
              println("H2O Random Forest FileInput example\n")
             
              if (args.length==0) {
                println("Input file missing, please pass datafile as the first parameter")
                return
              }
              
              // Start H2O instance and wait for 3 seconds for instance to complete startup
              println("Starting H2O")
              val h2oargs = "-nthreads -1 -quiet" 
              
              H2OApp.main(h2oargs.split(" "))
              H2O.waitForCloudSize(1, 3000) 
              
              println("H2O available")
              
              // Load datafile passed as first parameter and print the size of the file as confirmation
              println("Loading data from file ")
              val inputfile = args(0)
              val parmsfile = args(1)
              def ignore: Boolean = System.getProperty("ignore","false").toBoolean
              
              val datafile = NFSFileVec.make(inputfile)
              val dataframe = ParseDataset.parse(Key.make("dataset-key") , datafile._key)
              println("Loaded file "+inputfile+" size "+datafile.byteSize()+" Cols:"+dataframe.numCols()+" Rows:"+dataframe.numRows())
              
              println(dataframe.anyVec().get_type_str)
              
              for (v <- 0 to dataframe.numCols()-1) {
                println(dataframe.name(v))
              }
              
              val c = dataframe.find("bad_loan")
              dataframe.replace(c, dataframe.vecs()(c).toCategoricalVec())
              
              // drop the id and member_id columns from model
              dataframe.remove(dataframe.find("id"))
              dataframe.remove(dataframe.find("member_id"))
              
              
              // set Random Forest parameters
              println("creating model parameters")
              var modelparams = new DRFParameters()
              var fields = modelparams.getClass.getFields
              
              for (line <- Source.fromFile(parmsfile).getLines) {
                  println("Reading parameter from file: "+line)
                  var linedata = line.split(" ")
                 

                 for(v <- fields){
                   if ( v.getName.matches(linedata(0))) {
                     val method1 = v.getDeclaringClass.getDeclaredField(linedata(0) )
                     method1.setAccessible(true)
                     println("Found "+linedata(0)+" Var "+v+" Accessable "+method1.isAccessible()+" Type "+method1.getType )
                     v.setAccessible(true)
                     v.setInt(modelparams, linedata(1).toInt)
                   } 
                 }       
              }
                  
              
              // hard coded values
              modelparams._train = dataframe._key
              modelparams._response_column = "bad_loan"

               if (ignore) {
                 println("Adding fields to ignore from file "+parmsfile+"FieldtoIgnore")
                 var ignoreNames = new Array[String](dataframe.numCols())
                 var in=0
                 for (line <- Source.fromFile(parmsfile+"FieldtoIgnore").getLines) {
                   ignoreNames(in) = line
                   in+=1
                 }
                 modelparams._ignored_columns=ignoreNames
               }


              println("Parameters set ")
              
              // train model
              println("Starting training")
              var job: DRF = new DRF(modelparams)
              var model: DRFModel = job.trainModel().get()
             
              println("Training completed")
              
              // training metrics
              println(model._output.toString())
              println("Model AUC: "+model.auc())
              println(model._output._variable_importances)
             
              // If you want to look at variables that are important and then model on them
              // the following will write them out, then use only those in other model training
              // handy when you have a thousand columns but want to train on only the important ones.
              // Then before calling the model... call modelparams._ignored_columns= Array("inq_last_6mths")
              // FileWriter

               if (ignore) {
                 val file = new FileOutputStream(parmsfile + "FieldtoIgnore")

                 var n = 0
                 var in = 0
                 var ignoreNames = new Array[String](dataframe.numCols())
                 val fieldnames = model._output._varimp._names
                 println("Fields to add to _ignored_columns field")
                 for (i <- model._output._varimp.scaled_values()) {
                   if (i < 0.3) {
                     println(n + " = " + fieldnames(n) + " = " + i)
                     Console.withOut(file) {
                       println(fieldnames(n))
                     }
                     ignoreNames(in) = fieldnames(n)
                     in += 1
                   }
                   n += 1
                 }
                 println("Drop these:")
                 for (i <- 0 to in) {
                   println(fieldnames(i))
                 }
                 file.close()
                 println()
               }
              
              // save model 
              var outputfile = inputfile+"_model_pojo.txt"
              var modeloutput: FileOutputStream = new FileOutputStream(outputfile)
              println("Saving model to "+outputfile)
              model.toJava(modeloutput, false, true)
              modeloutput.close()
              
              outputfile = inputfile+"_model_jason.txt"
              modeloutput = new FileOutputStream(outputfile)
              println("Saving Jason to "+outputfile)
              Console.withOut(modeloutput) {  println(model.toJsonString()) }
              modeloutput.close()
                
              outputfile = inputfile+"_model_mojo.zip"
              modeloutput = new FileOutputStream(outputfile)
              println("Saving mojo to "+outputfile)
              model.getMojo.writeTo(modeloutput)
              modeloutput.close()

               println(models: hex.ensemble.StackedEnsemble )
             
              println("Completed")
              H2O.shutdown(0)
           
          }
        }

        
步骤2： 编译和运行MOJO
''''''''''''''''''''''''''''''''

1. 打开一个 *新* 终端窗口，将目录更改为 **experiment** 文件夹：
 
   .. code:: java

       $ cd experiment

2. 通过在 **experiment** 文件夹创建一个main.java文件的新文件来创建您的主程序（例如，使用 "vim main.java"），该文件包含以下内容。注意，这个文件引用了上面使用R创建的GBM模型。

   .. code:: java

       import java.io.*;
       import hex.genmodel.easy.RowData;
       import hex.genmodel.easy.EasyPredictModelWrapper;
       import hex.genmodel.easy.prediction.*;
       import hex.genmodel.MojoModel;

       public class main {
         public static void main(String[] args) throws Exception {
           EasyPredictModelWrapper model = new EasyPredictModelWrapper(MojoModel.load("GBM_model_R_1475248925871_74.zip"));

           RowData row = new RowData();
           row.put("AGE", "68");
           row.put("RACE", "2");
           row.put("DCAPS", "2");
           row.put("VOL", "0");
           row.put("GLEASON", "6");

           BinomialModelPrediction p = model.predictBinomial(row);
           System.out.println("Has penetrated the prostatic capsule (1=yes; 0=no): " + p.label);
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

 GBM和DRF会返回classProbabilities，但是不是所有MOJOs都会返回classProbabilities属性。参考每个算法的模型预测定义，找到要访问的正确字段。这可以在AIR-3的GitHub repo上找到：https://github.com/Shysoong/air-3/tree/master/h2o-genmodel/src/main/java/hex/genmodel/easy/prediction。您也可以在 `Javadoc <http://docs.h2o.ai/h2o/latest-stable/h2o-genmodel/javadoc/index.html>`__ 查看hex.genmodel.easy.prediction包下的类。

 除了classProbabilities，在GBM、DRF、Isolation Forest and XGBoost中，您还可以选择生成 ``leafNodeAssignments`` 字段，在GBM、DRF和XGBoost中选择生成 ``contributions`` 字段。 ``leafNodeAssignments`` 字段将显示通过每棵树的决策路径， ``contributions`` 字段将提供Shapley贡献。注意，这可能会降低MOJO的速度，因为它会增加计算量。下面的Java代码显示了如何返回叶节点赋值和贡献度:

 .. code:: java

     import java.io.*;
     import hex.genmodel.easy.RowData;
     import hex.genmodel.easy.EasyPredictModelWrapper;
     import hex.genmodel.easy.prediction.*;
     import hex.genmodel.MojoModel;

     public class main {
       public static void main(String[] args) throws Exception {
         EasyPredictModelWrapper.Config config = new EasyPredictModelWrapper.Config()
            .setModel(MojoModel.load("GBM_model_R_1475248925871_74.zip"))
            .setEnableLeafAssignment(true)
            .setEnableContributions(true);
         EasyPredictModelWrapper model = new EasyPredictModelWrapper(config);

         RowData row = new RowData();
         row.put("AGE", "68");
         row.put("RACE", "2");
         row.put("DCAPS", "2");
         row.put("VOL", "0");
         row.put("GLEASON", "6");

         BinomialModelPrediction p = model.predictBinomial(row);
         System.out.println("Has penetrated the prostatic capsule (1=yes; 0=no): " + p.label);
         System.out.print("Class probabilities: ");
         for (int i = 0; i < p.classProbabilities.length; i++) {
           if (i > 0) {
             System.out.print(",");
           }
           System.out.print(p.classProbabilities[i]);
         }

         System.out.println("Leaf node assignments: ");
         for (int i=0; i < p.leafNodeAssignments; i++) {
           if (i > 0) {
             System.out.print(p.leafNodeAssignments[i]);
           }
         }
         System.out.println("");

         System.out.println("Shapley contributions: ");
         for (int i=0; i < p.contributions; i++) {
           if (i > 0) {
             System.out.print(",");
           }
           System.out.print(p.contributions[i]);
         }
         System.out.println("");
       }
     }

 对于GLRM，默认情况下返回的字段是原型的X系数。此外，还可以选择生成重构的数据行。同样，由于增加了计算，这可能会降低MOJO的速度。下面的Java代码展示了如何在生成GLRM MOJO之后获得X系数和重构数据：

 .. code:: java

     import java.io.*;
     import hex.genmodel.easy.RowData;
     import hex.genmodel.easy.EasyPredictModelWrapper;
     import hex.genmodel.easy.prediction.*;
     import hex.genmodel.MojoModel;

     public class main {
       public static void main(String[] args) throws Exception {
       EasyPredictModelWrapper.Config config = new EasyPredictModelWrapper.Config().setModel(MojoModel.load("GLRM_model_python_1530295749484_1.zip")).setEnableGLRMReconstrut(true);
       EasyPredictModelWrapper model = new EasyPredictModelWrapper(config);

       RowData row = new RowData();
       row.put("CAPSULE", "0");
       row.put("AGE", "68");
       row.put("RACE", "2");
       row.put("DPROS", "4");
       row.put("DCAPS", "2");
       row.put("PSA", "31.9");
       row.put("VOL", "0");
       row.put("GLEASON", "6");

       DimReductionModelPrediction p = model.predictDimReduction(row);
       String[] colnames = model.m.getNames();
       System.out.println("X coefficients for input row: ");
       for (int i = 0; i < p.dimensions.length; i++) {
         if (i > 0) {
           System.out.println(",");
         }
         System.out.print("Arch "+i+" coefficient: "+p.dimensions[i]);
       }
       System.out.println("");
       System.out.println("Reconstructed input row: ");
       for (int i = 0; i < p.reconstructed.length; i++) {
         if (i > 0) {
           System.out.println(",");
         }
         System.out.print(colnames[i]+": "+p.reconstructed[i]);
       }
       System.out.println("");
     }


3. 在终端窗口2编译。

   .. code:: bash

       $ javac -cp h2o-genmodel.jar -J-Xms2g -J-XX:MaxPermSize=128m main.java

4. 在终端窗口2运行。

   .. code:: bash

       # Linux and OS X users
       $ java -cp .:h2o-genmodel.jar main 

       # Windows users
       $ java -cp .;h2o-genmodel.jar main  

 下列输出会被打印出来：

 .. code:: bash

  Has penetrated the prostatic capsule (1 yes; 0 no): 0
  Class probabilities: 0.8059929056296662,0.19400709437033375

 如果选择启用叶节点赋值，还将看到数据行有100个叶节点赋值：

 .. code:: bash

  Has penetrated the prostatic capsule (1 yes; 0 no): 0
  Class probabilities: 0.8059929056296662,0.19400709437033375
  Leaf node assignments:   RRRR,RRR,RRRR,RRR,RRL,RRRR,RLRR,RRR,RRR,RRR,RLRR,...

 对于GLRM MOJO，在运行Java代码之后，您将看到以下内容：

 .. code:: java

  X coefficients for input row:
  Arch 0 coefficient: -0.5930494611027051,
  Arch 1 coefficient: 1.0459847877909487,
  Arch 2 coefficient: 0.5849220609025815
  Reconstructed input row:
  CAPSULE: 0.5204822003860688,
  AGE: 10.520294102886806,
  RACE: 4.1422863477607645,
  DPROS: 2.970424071063664,
  DCAPS: 6.361196172145799,
  PSA: 1.905415090602722,
  VOL: 0.7123169431687857,
  GLEASON: 6.625024806196047

查看MOJO模型
~~~~~~~~~~~~~~~~~~~~

一个用于将二进制MOJO文件转换为人类可读图形的java工具打包在了AIR中。该工具生成的输出的"点"(Graphviz的一部分)可以转换成图像。(查看 `Graphviz官网 <http://www.graphviz.org/>`__ 以了解更多信息。)

下面是一个GBM模型的示例输出：

.. figure:: images/gbm_mojo_graph.png
   :alt: GBM MOJO model

下面的代码片段展示了如何通过R下载MOJO并在命令行上运行PrintMojo工具来生成.png文件。为了更好地控制树的外观和感觉，我们为PrintMojo提供了两个选项：

- ``--decimalplaces`` (或 ``-d``) 允许您控制数字显示的小数点数目。
- ``--fontsize`` (或 ``-f``) 控制字体大小。默认字体大小是14。使用此选项时，请注意不要选择太大字体而无法看到整个树。我们建议使用不大于20的字体。

.. tabs::
   .. code-tab:: r R

    library(h2o)
    h2o.init()
    df <- h2o.importFile("http://s3.amazonaws.com/h2o-public-test-data/smalldata/airlines/allyears2k_headers.zip")
    model <- h2o.gbm(model_id = "model",
                    training_frame = df,
                    x = c("Year", "Month", "DayofMonth", "DayOfWeek", "UniqueCarrier"),
                    y = "IsDepDelayed",
                    max_depth = 3,
                    ntrees = 5)
    h2o.download_mojo(model, getwd(), FALSE)

    # Now download the latest stable h2o release from http://www.h2o.ai/download/
    # and run the PrintMojo tool from the command line.
    #
    # (For MacOS: brew install graphviz)
    java -cp h2o.jar hex.genmodel.tools.PrintMojo --tree 0 -i model.zip -o model.gv -f 20 -d 3
    dot -Tpng model.gv -o model.png
    open model.png

FAQ
~~~
-  **MOJOs是线程安全的吗？**

  是的，平台内所有的MOJOs都是线程安全的。

-  **如何在Maven中使用XGBoost MOJO?**

  如果您声明了对h2o-genmodel的依赖关系，那么如果您计划使用XGBoost模型，还必须包含h2o-genmodel-ext-xgboost依赖关系。例如：

  ::

    <groupId>ai.h2o</groupId>
    <artifactId>xgboost-mojo-example</artifactId>
    <version>1.0-SNAPSHOT</version>

    dependency>
        <groupId>ai.h2o</groupId>
        <artifactId>h2o-genmodel-ext-xgboost</artifactId>
        <version>3.18.0.8</version>
    </dependency>
    <dependency>
        <groupId>ai.h2o</groupId>
        <artifactId>h2o-genmodel</artifactId>
        <version>3.18.0.8</version>
    </dependency>
