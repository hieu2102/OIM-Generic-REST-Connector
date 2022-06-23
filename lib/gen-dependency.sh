#!/bin/bash
find info -name "*MANIFEST.MF"|while read line 
do 
    echo
    echo "================${line}===================="
    groupid="$(grep -E "Bundle-SymbolicName|Automatic-Module-Name" ${line}|head -n 1|awk '{print $NF}')"
    artifactid=$(echo ${groupid//./ }|awk '{print $NF}')
    version=$(grep Specification-Version ${line}|awk '{print $NF}')
    echo "group=$groupid artifact=$artifactid version=$version"
done 