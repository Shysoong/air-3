将数据分割为训练集/测试集/验证集
---------------------------------------------------

这个示例展示了如何将单个数据集拆分为两个数据集，一个用于训练，另一个用于测试。

注意，当分割帧时，AIR不会给出一个精确的分割。它被设计成使用概率分裂方法而不是精确分裂来高效处理大数据。例如，当指定0.75/0.25分割，AIR将生成一个期望值为0.75/0.25的测试/训练分割，而不是恰好为0.75/0.25。在小数据集上，分割结果的大小将比大数据更偏离期望值，因为大数据非常接近于精确值。

.. tabs::
   .. code-tab:: r R
   
		library(h2o)
		h2o.init()
		
		# Import the prostate dataset
		prostate.hex <- h2o.importFile(path = "http://h2o-public-test-data.s3.amazonaws.com/smalldata/prostate/prostate.csv", destination_frame = "prostate.hex")
		print(dim(prostate.hex))
		[1] 380   9 
		
		# Split dataset giving the training dataset 75% of the data
		prostate.split <- h2o.splitFrame(data=prostate.hex, ratios=0.75)
		print(dim(prostate.split[[1]]))
		[1] 291   9
		print(dim(prostate.split[[2]]))
		[1] 89  9
		
		# Create a training set from the 1st dataset in the split
		prostate.train <- prostate.split[[1]]
		
		# Create a testing set from the 2nd dataset in the split
		prostate.test <- prostate.split[[2]]
		
		# Generate a GLM model using the training dataset. x represesnts the predictor column, and y represents the target index.
		prostate.glm <- h2o.glm(y = "CAPSULE", 
		                        x = c("AGE", "RACE", "PSA", "DCAPS"), 
		                        training_frame=prostate.train, 
		                        family="binomial", 
		                        nfolds=10, 
		                        alpha=0.5)
		
		# Predict using the GLM model and the testing dataset
		pred = h2o.predict(object=prostate.glm, newdata=prostate.test)
		
		# View a summary of the prediction with a probability of TRUE
		summary(pred$p1, exact_quantiles=TRUE)
		p1
		Min.   :0.1560
		1st Qu.:0.2954
		Median :0.3535
		Mean   :0.4111
		3rd Qu.:0.4369
		Max.   :0.9989 

   .. code-tab:: python

		import h2o
		from h2o.estimators.glm import H2OGeneralizedLinearEstimator
		h2o.init()
		
		# Import the prostate dataset
		prostate = "http://h2o-public-test-data.s3.amazonaws.com/smalldata/prostate/prostate.csv"
		prostate_df = h2o.import_file(path=prostate)
		
		# Split the data into Train/Test/Validation with Train having 70% and test and validation 15% each
		train,test,valid = prostate_df.split_frame(ratios=[.7, .15])
		
		# Generate a GLM model using the training dataset
		glm_classifier = H2OGeneralizedLinearEstimator(family="binomial", nfolds=10, alpha=0.5)
		glm_classifier.train(y="CAPSULE", x=["AGE", "RACE", "PSA", "DCAPS"], training_frame=train)
		
		# Predict using the GLM model and the testing dataset
		predict = glm_classifier.predict(test)
		
		# View a summary of the prediction
		predict.head()
		  predict        p0        p1
		---------  --------  --------
		        1  0.366189  0.633811
		        1  0.351269  0.648731
		        1  0.69012   0.30988
		        0  0.762335  0.237665
		        1  0.680127  0.319873
		        1  0.687736  0.312264
		        1  0.676753  0.323247
		        1  0.685876  0.314124
		        1  0.707027  0.292973
		        0  0.74706   0.25294
		
		[10 rows x 3 columns]
