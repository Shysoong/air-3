package water.hadoop;

import water.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

class EmbeddedH2OConfig extends water.init.AbstractEmbeddedH2OConfig {

  private static final int FETCH_FILE_RETRYS = Integer.parseInt(System.getProperty("sys.ai.h2o.hadoop.callback.retrys", "3")); 

  private volatile String _driverCallbackIp;
  private volatile int _driverCallbackPort = -1;
  private volatile int _mapperCallbackPort = -1;
  private volatile String _embeddedWebServerIp = "(Unknown)";
  private volatile int _embeddedWebServerPort = -1;

  void setDriverCallbackIp(String value) {
    _driverCallbackIp = value;
  }

  void setDriverCallbackPort(int value) {
    _driverCallbackPort = value;
  }

  void setMapperCallbackPort(int value) {
    _mapperCallbackPort = value;
  }

  private class BackgroundWriterThread extends Thread {
    MapperToDriverMessage _m;

    void setMessage (MapperToDriverMessage value) {
      _m = value;
    }

    public void run() {
      try (Socket s = new Socket(_m.getDriverCallbackIp(), _m.getDriverCallbackPort())) {
        _m.write(s);
      }
      catch (java.net.ConnectException e) {
        System.out.println("EmbeddedH2OConfig: BackgroundWriterThread could not connect to driver at " + _driverCallbackIp + ":" + _driverCallbackPort);
        System.out.println("(This is normal when the driver disowns the hadoop job and exits.)");
      }
      catch (Exception e) {
        System.out.println("EmbeddedH2OConfig: BackgroundWriterThread caught an Exception");
        e.printStackTrace();
      }
    }
  }

  void setEmbeddedWebServerInfo(String ip, int port) {
    _embeddedWebServerIp = ip;
    _embeddedWebServerPort = port;
  }
  
  @Override
  public void notifyAboutEmbeddedWebServerIpPort(InetAddress ip, int port) {
    setEmbeddedWebServerInfo(ip.getHostAddress(), port);

    try {
      MapperToDriverMessage msg = new MapperToDriverMessage();
      msg.setDriverCallbackIpPort(_driverCallbackIp, _driverCallbackPort);
      msg.setMessageEmbeddedWebServerIpPort(ip.getHostAddress(), port);
      BackgroundWriterThread bwt = new BackgroundWriterThread();
      System.out.printf("EmbeddedH2OConfig: notifyAboutEmbeddedWebServerIpPort called (%s, %d)\n", ip.getHostAddress(), port);
      bwt.setMessage(msg);
      bwt.start();
    }
    catch (Exception e) {
      System.out.println("EmbeddedH2OConfig: notifyAboutEmbeddedWebServerIpPort caught an Exception");
      e.printStackTrace();
    }
  }

  @Override
  public boolean providesFlatfile() {
    return true;
  }

  @Override
  public String fetchFlatfile() throws Exception {
    System.out.println("EmbeddedH2OConfig: fetchFlatfile called");
    DriverToMapperMessage response = null;
    for (int i = 0; i < FETCH_FILE_RETRYS; i++) {
      try {
        System.out.println("EmbeddedH2OConfig: Attempting to fetch flatfile (attempt #" + i + ")");
        MapperToDriverMessage msg = new MapperToDriverMessage();
        msg.setMessageFetchFlatfile(_embeddedWebServerIp, _embeddedWebServerPort);
        Socket s = new Socket(_driverCallbackIp, _driverCallbackPort);
        msg.write(s);
        response = new DriverToMapperMessage();
        response.read(s);
        s.close();
        break;
      } catch (IOException ioex) {
        if (i + 1 == FETCH_FILE_RETRYS)
          throw ioex;
        reportFetchfileAttemptFailure(ioex, i);
      }
    }
    assert response != null;
    char type = response.getType();
    if (type != DriverToMapperMessage.TYPE_FETCH_FLATFILE_RESPONSE) {
      int typeAsInt = (int)type & 0xff;
      String str = "DriverToMapperMessage type unrecognized (" + typeAsInt + ")";
      Log.err(str);
      throw new Exception(str);
    }
    String flatfile = response.getFlatfile();
    System.out.println("EmbeddedH2OConfig: fetchFlatfile returned");
    System.out.println("------------------------------------------------------------");
    System.out.println(flatfile);
    System.out.println("------------------------------------------------------------");
    return flatfile;
  }

  protected void reportFetchfileAttemptFailure(IOException ioex, int attempt) throws IOException {
    System.out.println("EmbeddedH2OConfig: Attempt #" + attempt + " to fetch flatfile failed");
    ioex.printStackTrace();
  }
  
  @Override
  public void notifyAboutCloudSize(InetAddress ip, int port, InetAddress leaderIp, int leaderPort, int size) {
    _embeddedWebServerIp = ip.getHostAddress();
    _embeddedWebServerPort = port;

    try {
      MapperToDriverMessage msg = new MapperToDriverMessage();
      msg.setDriverCallbackIpPort(_driverCallbackIp, _driverCallbackPort);
      msg.setMessageCloudSize(ip.getHostAddress(), port, leaderIp.getHostAddress(), leaderPort, size);
      BackgroundWriterThread bwt = new BackgroundWriterThread();
      System.out.printf("EmbeddedH2OConfig: notifyAboutCloudSize called (%s, %d, %d)\n", ip.getHostAddress(), port, size);
      bwt.setMessage(msg);
      bwt.start();
    }
    catch (Exception e) {
      System.out.println("EmbeddedH2OConfig: notifyAboutCloudSize caught an Exception");
      e.printStackTrace();
    }
  }

  @Override
  public void exit(int status) {
    try {
      MapperToDriverMessage msg = new MapperToDriverMessage();
      msg.setDriverCallbackIpPort(_driverCallbackIp, _driverCallbackPort);
      msg.setMessageExit(_embeddedWebServerIp, _embeddedWebServerPort, status);
      System.out.printf("EmbeddedH2OConfig: exit called (%d)\n", status);
      BackgroundWriterThread bwt = new BackgroundWriterThread();
      bwt.setMessage(msg);
      bwt.start();
      System.out.println("EmbeddedH2OConfig: after bwt.start()");
    }
    catch (Exception e) {
      System.out.println("EmbeddedH2OConfig: failed to send message to driver");
      e.printStackTrace();
    }

    Socket s = null;
    try {
      Thread.sleep(1000);
      // Wait one second to deliver the message before exiting.
      s = new Socket("127.0.0.1", _mapperCallbackPort);
      byte[] b = new byte[] { (byte) status };
      OutputStream os = s.getOutputStream();
      os.write(b);
      os.flush();
      s.close();
      s = null;
      System.out.println("EmbeddedH2OConfig: after write to mapperCallbackPort");

      Thread.sleep(60 * 1000);
      // Should never make it this far!
    } catch (Exception e) {
      System.out.println("EmbeddedH2OConfig: exit caught an exception 2");
      e.printStackTrace();
    } finally {
      if (s != null) {
        try {
          s.close();
        } catch (IOException e) {
          // ignore
        }
      }
    }

    System.exit(111);
  }

  @Override
  public void print() {
    System.out.println("EmbeddedH2OConfig print()");
    System.out.println("    Driver callback IP: " + ((_driverCallbackIp != null) ? _driverCallbackIp : "(null)"));
    System.out.println("    Driver callback port: " + _driverCallbackPort);
    System.out.println("    Embedded webserver IP: " + ((_embeddedWebServerIp != null) ? _embeddedWebServerIp : "(null)"));
    System.out.println("    Embedded webserver port: " + _embeddedWebServerPort);
  }
}
