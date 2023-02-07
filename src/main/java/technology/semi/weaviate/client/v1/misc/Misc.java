package technology.semi.weaviate.client.v1.misc;

import technology.semi.weaviate.client.Config;
import technology.semi.weaviate.client.base.http.HttpClient;
import technology.semi.weaviate.client.base.util.DbVersionProvider;
import technology.semi.weaviate.client.v1.misc.api.LiveChecker;
import technology.semi.weaviate.client.v1.misc.api.MetaGetter;
import technology.semi.weaviate.client.v1.misc.api.OpenIDConfigGetter;
import technology.semi.weaviate.client.v1.misc.api.ReadyChecker;

public class Misc {
  private final Config config;
  private final DbVersionProvider dbVersionProvider;
  private final HttpClient httpClient;

  public Misc(HttpClient httpClient, Config config, DbVersionProvider dbVersionProvider) {
    this.httpClient = httpClient;
    this.config = config;
    this.dbVersionProvider = dbVersionProvider;
  }

  public MetaGetter metaGetter() {
    return new MetaGetter(httpClient, config);
  }

  public OpenIDConfigGetter openIDConfigGetter() {
    return new OpenIDConfigGetter(httpClient, config);
  }

  public LiveChecker liveChecker() {
    return new LiveChecker(httpClient, config, dbVersionProvider);
  }

  public ReadyChecker readyChecker() {
    return new ReadyChecker(httpClient, config, dbVersionProvider);
  }
}
