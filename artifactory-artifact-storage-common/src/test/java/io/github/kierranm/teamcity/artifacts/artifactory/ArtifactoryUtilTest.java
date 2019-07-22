package io.github.kierranm.teamcity.artifacts.artifactory;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;

public class ArtifactoryUtilTest {
  @DataProvider
  public Object[][] getContentTypeData() {
    return new Object[][]{
      {"file.css", "text/css"},
      {"file.zip", "application/zip"},
      {"file.txt", "text/plain"},
      {"file.jpg", "image/jpeg"},
      {"file.bin", "application/octet-stream"},
      {"file.htm", "text/html"},
      {"file.html", "text/html"},
    };
  }

  @Test(dataProvider = "getContentTypeData")
  public void getContentTypeTest(String fileName, String expectedType) {
    Assert.assertEquals(ArtifactoryUtil.getContentType(new File("ArtifactoryUtilsTest", fileName)), expectedType);
  }
}
