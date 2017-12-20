package org.hypergraphql.config.schema;

import org.hypergraphql.datafetching.services.Service;

public class FieldConfig {

    private String id;
  //  private Service service;

    public FieldConfig(String id, Service service) {

        this.id = id;
    //    this.service = service;

    }

    public String getId() { return this.id; }
  //  public Service getService() { return this.service; }

}
