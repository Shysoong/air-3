package hex.schemas;

import ai.h2o.automl.targetencoding.TargetEncoderModel;
import water.api.schemas3.ModelOutputSchemaV3;

public class TargetEncoderModelOutputV3 extends ModelOutputSchemaV3<TargetEncoderModel.TargetEncoderOutput, TargetEncoderModelOutputV3> {
  
  public double prior_mean;

  public TargetEncoderModelOutputV3() {
  }
  
}
