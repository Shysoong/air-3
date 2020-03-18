package hex.ensemble;

import hex.tree.xgboost.XGBoostModel.XGBoostParameters;

public class XGBoostMetalearnerProvider implements MetalearnerProvider<XGBoostMetalearnerProvider.XGBoostMetalearner> {

    static class XGBoostMetalearner extends Metalearners.SimpleMetalearner {
        public XGBoostMetalearner() {
            super(Algorithm.xgboost.name());
        }
    }

    @Override
    public String getName() {
        return Metalearner.Algorithm.xgboost.name();
    }

    @Override
    public XGBoostMetalearner newInstance() {
        return new XGBoostMetalearner();
    }
}
