#!/bin/bash
repo_path=$( cd "$(dirname "${BASH_SOURCE[0]}")" ; pwd -P )
cd "$parent_path"
rm -rf target/
sbt "run 9875 -Dapplication.router=testOnlyDoNotUseInAppConf.Routes -Dlogger.resource=logback-test.xml"