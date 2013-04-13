#!/bin/bash

JAR_PATH=$HOME

# Simple helper script for creating search result files
#
# Example:
# ./create_trec_files.sh path/to/20_newsgroups_subset medium.arff.gz medium
#
# This will create all search results for the 20 topics and store the files 
# in /tmp/output

collection=$1
index_arff=$2
plist_name=$3

if [ -z "$collection" ]; then
    echo "Usage ./create_trec_files.sh collection index.arff.gz (medium|small|large)";
    exit;
fi

if [ -z "$index_arff" ]; then
    echo "Usage ./create_trec_files.sh collection index.arff.gz (medium|small|large)";
    exit;
fi

if [ -z "$plist_name" ]; then
    echo "Usage ./create_trec_files.sh collection index.arff.gz (medium|small|large)";
    exit;
fi

topics="misc.forsale/76057 talk.religion.misc/83561 talk.politics.mideast/75422 sci.electronics/53720 sci.crypt/15725 misc.forsale/76165
talk.politics.mideast/76261 alt.atheism/53358 sci.electronics/54340 rec.motorcycles/104389 talk.politics.guns/54328 misc.forsale/76468 sci.crypt/15469 rec.sport.hockey/54171 talk.religion.misc/84177 rec.motorcycles/104727 comp.sys.mac.hardware/52165 sci.crypt/15379 sci.space/60779 sci.med/59456"

mkdir -p /tmp/output

i=1;
for topic in $topics;
    do
        out_file="/tmp/output/"$plist_name"_topic"$i"_group1.txt"
        eval "java -jar \"$JAR_PATH/indexer.jar\" -i \"$index_arff\" -t $i -lsize $plist_name -q \"$collection/$topic\" -searchout \"$out_file\"";
        ((i++));
done;
