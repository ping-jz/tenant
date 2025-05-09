package org.example.util;


import static org.example.util.Ok.Ok;

import org.junit.jupiter.api.Test;

public class ResultTest {

  @Test
  public void matchTest() throws Throwable {
    Result<String, Boolean> res = Ok("asdfasdf");

    switch (res) {
      case Err<String, Boolean> v -> {
      }
      case Ok<String, Boolean> v -> {
      }
    }
  }


}


