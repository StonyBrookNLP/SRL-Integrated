#!/bin/bash

cp -r $1 $2
rm -rf $2/cross-val-ablation/fold-2
rm -rf $2/cross-val-ablation/fold-3
rm -rf $2/cross-val-ablation/fold-4
rm -rf $2/cross-val-ablation/fold-5
