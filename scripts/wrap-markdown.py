#!/usr/bin/env python3
"""Word-wrap markdown paragraphs at given width, preserving code/tables/lists."""

import sys
import textwrap
import re

def wrap_file(path, width=120):
    with open(path) as f:
        lines = f.readlines()

    has_trailing_newline = lines and lines[-1].endswith('\n')
    cleaned = [l.rstrip('\n') for l in lines]
    result = []
    i = 0
    n = len(cleaned)

    while i < n:
        line = cleaned[i]

        # Fenced code block — copy verbatim
        if line.lstrip().startswith('```') or line.lstrip().startswith('~~~'):
            result.append(line)
            i += 1
            while i < n:
                result.append(cleaned[i])
                if cleaned[i].lstrip().startswith('```') or cleaned[i].lstrip().startswith('~~~'):
                    i += 1
                    break
                i += 1
            continue

        # Indented code block (4+ spaces) — copy verbatim
        if re.match(r'^( {4,}|\t)', line):
            result.append(line)
            i += 1
            continue

        # Table row — copy verbatim
        if '|' in line.strip() and (line.strip().startswith('|') or line.strip().endswith('|') or line.strip().startswith('|--')):
            result.append(line)
            i += 1
            continue

        # Blank line
        if not line.strip():
            result.append('')
            i += 1
            continue

        # Structural single-line elements — copy verbatim
        stripped = line.strip()
        is_structural_single = (
            stripped.startswith('#') or
            stripped == '---' or stripped == '___' or stripped == '***' or
            stripped.startswith('<!--') or
            re.match(r'^\[.*\]:\s', stripped) or  # link reference def
            stripped.startswith('![') or  # image
            stripped.startswith('[![')  # badge image link
        )
        if is_structural_single:
            result.append(line)
            i += 1
            continue

        # List item or blockquote — wrap individually
        list_match = re.match(r'^(\s*[-*+] |\s*\d+[.)] |\s*> )', line)
        if list_match:
            prefix = list_match.group(1)
            # Collect continuation lines (indented text following a list item)
            list_lines = [line]
            i += 1
            while i < n:
                next_line = cleaned[i]
                # Continuation: blank line before next item ends the list block
                if not next_line.strip():
                    # Check if next non-blank is a list item (then blank is separator)
                    peek = i + 1
                    while peek < n and not cleaned[peek].strip():
                        peek += 1
                    if peek < n and re.match(r'^(\s*[-*+] |\s*\d+[.)] )', cleaned[peek]):
                        # Blank line between list items is a separator - preserve it
                        list_lines.append('')
                        i += 1
                        continue
                    # End of list block
                    break
                # Don't grab structural elements
                if re.match(r'^( {4,}|\t)', next_line) or next_line.lstrip().startswith('```'):
                    break
                if '|' in next_line.strip() and (next_line.strip().startswith('|') or next_line.strip().endswith('|')):
                    break
                # Another list item at the same level or deeper ends this one
                if re.match(r'^(\s*[-*+] |\s*\d+[.)] )', next_line):
                    # This is a new list item — end current item
                    break
                list_lines.append(next_line)
                i += 1

            # Unwrap and rewrap the list content
            for li in list_lines:
                if not li.strip():
                    result.append('')
                else:
                    result.append(li)
            continue

        # Regular paragraph — collect lines, unwrap, rewrap
        para_lines = [line]
        i += 1
        while i < n:
            l = cleaned[i]
            if not l.strip():
                break
            # Stop at structural elements
            if (l.lstrip().startswith('```') or l.lstrip().startswith('~~~') or
                re.match(r'^( {4,}|\t)', l) or
                ('|' in l.strip() and (l.strip().startswith('|') or l.strip().endswith('|'))) or
                l.strip().startswith('#') or
                l.strip() == '---' or l.strip() == '___' or l.strip() == '***' or
                l.strip().startswith('<!--') or
                l.strip().startswith('[![') or
                re.match(r'^(\s*[-*+] |\s*\d+[.)] )', l) or
                re.match(r'^\[.*\]:\s', l.strip())):
                break
            para_lines.append(l)
            i += 1

        if not para_lines:
            continue

        # Unwrap into single text
        text = ' '.join(l.strip() for l in para_lines)
        text = re.sub(r' +', ' ', text)

        # Wrap
        wrapped = textwrap.fill(
            text,
            width=width,
            break_long_words=False,
            break_on_hyphens=False,
            replace_whitespace=False
        )
        result.append(wrapped)

    out = '\n'.join(result)
    if has_trailing_newline:
        out += '\n'

    with open(path, 'w') as f:
        f.write(out)


if __name__ == '__main__':
    files = sys.argv[1:] if len(sys.argv) > 1 else []
    for f in files:
        wrap_file(f)
        print(f"  Wrapped: {f}")
