package com.istudy.resources;

import java.io.IOException;
import java.io.InputStream;

/** Stores file bodies; domain records keep stable keys rather than public URLs. */
public interface ObjectStorage {
  InputStream read(String key) throws IOException;
  void write(String key, InputStream content) throws IOException;
}
