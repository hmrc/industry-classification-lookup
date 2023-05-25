#!/bin/bash

IGNORE_CHECK=${1:-DO_CHECK}

function deleteManagedResourcesAndRun() {
  echo "deleting managed resources"
  echo "-------------------------------------------------------"
  repo_path=$( cd "$(dirname "${BASH_SOURCE[0]}")" ; pwd -P )
  cd "$parent_path"
  rm -rf target/scala-2.12/resource_managed
  sbt "run 9875 -Dapplication.router=testOnlyDoNotUseInAppConf.Routes -Dlogger.resource=logback-test.xml"
}

if [ "$IGNORE_CHECK" == "IGNORE_CHECK" ]; then
  echo "-------------------------------------------------------"
  echo "Ignoring SIC code dataset check"
  echo "-------------------------------------------------------"
  deleteManagedResourcesAndRun
else
  . ./dataSetCheck.sh index-src/ONSSupplementDataSet.txt
  onsResult="$SIC_CHECK_RESULT"
  . ./dataSetCheck.sh index-src/GDSRegisterDataSet.txt
  gdsResult="$SIC_CHECK_RESULT"

  if [ "$onsResult" == "SUCCESS" ] && [ "$gdsResult" == "SUCCESS" ] ; then
    deleteManagedResourcesAndRun
  else
    echo ""
    echo "Run again with command './run.sh IGNORE_CHECK' to bypass SIC code checks"
  fi
fi
