def parse_file(fname):
    """For each sentence, extract each token's id, token, head and SRL labels"""
    with open(fname) as f:
        sentences = []
        sentence = []
        for line in f:
            line = line.strip()
            if line:
                entry = line.split("\t")
                tid = int(entry[0])
                head = int(entry[5])
                labels = entry[-1].split(";")
                if '_' in labels:
                    labels = []
                sentence.append([tid, entry[1], entry[2], entry[3], entry[4], head,
                                 entry[6], entry[7], labels])
            else:
                sentences.append(sentence)
                sentence = []
        if sentence:
            sentences.append(sentence)
    return sentences


def compress_sent(sentence, head_only=False):
    a0 = []
    a1 = []
    a2 = []
    pred = []
    for sent in sentence:
        tid = sent[0]
        head = sent[5]
        labels = sent[-1]
        for label in labels:
            if (head_only and label in sentence[head - 1][3]) :
                continue
            l_head, role = label.split(":")
            l_head = int(l_head)
            if l_head not in pred:
                pred.append(l_head)
            if role == 'A0' and tid not in a0:
                a0.append(tid)
            elif role == 'A1' and tid not in a1:
                a1.append(tid)
            elif role == 'A2' and tid not in a2:
                a2.append(tid)

    compressed = [sorted(a0), sorted(a1),
                  sorted(a2), sorted(pred)]
    return compressed


def format_role(role_list, sentence):
    prev = None
    tokens = []
    for tid in role_list:
        token = sentence[tid - 1][1]
        if prev and tid != prev + 1:
            tokens.append("|")
        tokens.append(token)
        prev = tid
    return " ".join(tokens)


def compress(sentences, head_only=False):
    compressed_sents = []
    for sentence in sentences:
        compressed = compress_sent(sentence, head_only)
        compressed_sents.append(compressed)
    return compressed_sents


def evaluate_role(gs, srl, role_id):
    tp = 0.0
    fp = 0.0
    fn = 0.0
    for i in range(len(gs)):
        gs_tokens = [t for t in gs[i][role_id]]
        srl_tokens = srl[i][role_id]
        for t in srl_tokens:
            if t in gs_tokens:
                tp += 1
                gs_tokens.remove(t)
            else:
                fp += 1
        fn += len(gs_tokens)
    if (tp + fp) > 0:
        #print "{}\t{}\t{}".format(role_id, tp, fp)
        precision = tp / (tp + fp)
    else:
        precision = "NaN"
    if (tp + fn) > 0:
        recall = tp / (tp + fn)
    else:
        recall = "NaN"
    return precision, recall


def evaluate(gs, srl):
    roles = ["A0", "A1", "A2"]
    print "\tP\tR\tF1"
    for i in range(len(roles)):
        p, r = evaluate_role(gs, srl, i)
        if p == "NaN" or r == "NaN":
            f1 = "-"
        elif p == 0 and r == 0:
            f1 = "NaN"
        else:
            f1 = 2 * (p * r) / (p + r)
        print "{}\t{}\t{}\t{}".format(roles[i], p, r, f1)


def write_to_file(comp, sents, fname):
    with open(fname, 'w') as f:
        for i in range(len(comp)):
            for entry in sents[i]:
                new_labels = []
                tid = entry[0]
                if tid in comp[i][0]:
                    new_labels.append("A0")
                if tid in comp[i][1]:
                    new_labels.append("A1")
                if tid in comp[i][2]:
                    new_labels.append("A2")
                if tid in comp[i][3]:
                    new_labels.append("T")
                if not new_labels:
                    new_labels = ["_"]
                entry[-1] = ";".join(new_labels)
                l = "{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}\n".format(entry[0], entry[1],
                                                                  entry[2], entry[3],
                                                                  entry[4], entry[5],
                                                                  entry[6], entry[7],
                                                                  entry[8])
                f.write(l)
            f.write("\n")


def main(gs_file, srl_file, head_only=False, output=False):
    gs_sents = parse_file(gs_file)
    srl_sents = parse_file(srl_file)
    gs_comp = compress(gs_sents, head_only)
    srl_comp = compress(srl_sents, head_only)
    evaluate(gs_comp, srl_comp)
    if output:
        write_to_file(gs_comp, gs_sents, "gs_compressed.txt")
        write_to_file(srl_comp, srl_sents, "srl_compressed.txt")

if __name__ == "__main__":
    import argparse

    parser = argparse.ArgumentParser(description='Evaluate SRL output as process frame.')
    parser.add_argument('gs_file', help='path to gold standard file')
    parser.add_argument('srl_file', help='path to srl predicted output file')
    parser.add_argument('--heads', action='store_true', help='Only evaluate the heads of the subtrees')
    parser.add_argument('--output', action='store_true', help='Output the compressed results to a file (gs_compressed.txt for gold standard; srl_compressed.txt for predicted)')

    args = parser.parse_args()

    main(args.gs_file, args.srl_file, head_only=args.heads, output=args.output)
