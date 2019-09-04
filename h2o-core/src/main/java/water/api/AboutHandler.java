package water.api;

import water.H2O;
import water.H2OSecurityManager;
import water.api.schemas3.AboutEntryV3;
import water.api.schemas3.AboutV3;
import water.util.PrettyPrint;

import java.util.ArrayList;
import java.util.Date;

public class AboutHandler extends Handler {

  @SuppressWarnings("unused") // called through reflection by RequestServer
  public AboutV3 get(int version, AboutV3 s) {
    ArrayList<AboutEntryV3> entries = new ArrayList<>();
    entries.add(new AboutEntryV3("构建基于git分支",      H2O.ABV.branchName()));
    entries.add(new AboutEntryV3("构建基于git提交哈希值",        H2O.ABV.lastCommitHash()));
    entries.add(new AboutEntryV3("构建基于git版本说明",    H2O.ABV.describe()));
    entries.add(new AboutEntryV3("构建工程版本", H2O.ABV.projectVersion()));
    entries.add(new AboutEntryV3("构建年龄",             PrettyPrint.toAge(H2O.ABV.compiledOnDate(), new Date())));
    entries.add(new AboutEntryV3("构建人",              H2O.ABV.compiledBy()));
    entries.add(new AboutEntryV3("构建时间",              H2O.ABV.compiledOn()));
    entries.add(new AboutEntryV3("内部安全", H2OSecurityManager.instance().securityEnabled ? "启用": "未启用"));

    if (H2O.ABV.isTooOld()) {
      entries.add(new AboutEntryV3("版本警告",
                                   "您的AIR版本过低！请从官网下载最新的版本http://www.skyease.io/download/"));
    }

    for (H2O.AboutEntry ae : H2O.getAboutEntries()) {
      entries.add(new AboutEntryV3(ae.getName(), ae.getValue()));
    }

    s.entries = entries.toArray(new AboutEntryV3[entries.size()]);
    return s;
  }
}
