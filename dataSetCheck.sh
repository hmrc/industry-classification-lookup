#!/bin/bash

#this script checks the file for SIC codes which are not 5 digits long

FILENAME=${1:-index-src/ONSSupplementDataSet.txt}

function sicsWithXDigits() {
  numOfSics=$(egrep "\b[0-9]{$1}	" $FILENAME -o | grep -c ^)
}

linesInFile=$(grep -c ^ $FILENAME)
sicsWithXDigits 5
sic5Digits=$numOfSics
numOfNon5DigitSics=$((linesInFile-sic5Digits))

echo ""
echo "-------------------------------------------------------"
echo "SIC code dataset check"
echo "-------------------------------------------------------"
echo "checking file: $FILENAME"
echo "lines in file: $linesInFile"
echo "5 digit SIC codes: $sic5Digits"
echo ""

function printSics() {
  sicsWithXDigits $1
  if [ $numOfSics -ne 0 ]; then
    sics=$(egrep "\b[0-9]{$1}	" $FILENAME -o)
    echo ""
    echo "- $1 digit SIC codes: $numOfSics"
    echo "$sics"
  fi
}

if [ $numOfNon5DigitSics -ne 0 ]; then

  echo "*******************************************************"
  echo "FAIL: $numOfNon5DigitSics SIC codes did not match the regex [0-9]{5}"
    printSics 1
    printSics 2
    printSics 3
    printSics 4
    printSics 6
    printSics 7
    printSics 8
    printSics 9
    printSics 10
  echo "*******************************************************"

 SIC_CHECK_RESULT="FAIL"
else
  echo "*******************************************************"
  echo "SUCCESS: all SIC codes match the regex [0-9]{5}"
  echo "*******************************************************"

  SIC_CHECK_RESULT="SUCCESS"
fi
echo ""
export SIC_CHECK_RESULT
