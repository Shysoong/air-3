#!/usr/bin/env python
# -*- encoding: utf-8 -*-
import sys, os
sys.path.insert(1, os.path.join("..", "..", "..", "h2o-py"))
import h2o
from h2o.exceptions import H2OServerError
from tests import pyunit_utils


def trace_request():
    err = None
    try:
        h2o.api("TRACE /")
    except H2OServerError as e:
        err = e

    msg = str(err.message)

    assert err is not None
    print("<Error message>")
    print(msg)
    print("</Error Message>")

    # exact message depends on Jetty Version
    assert (msg.startswith("HTTP 500") and "TRACE method is not supported" in msg) or \
           msg.startswith("HTTP 405 Method Not Allowed")


if __name__ == "__main__":
    pyunit_utils.standalone_test(trace_request)
else:
    trace_request()
