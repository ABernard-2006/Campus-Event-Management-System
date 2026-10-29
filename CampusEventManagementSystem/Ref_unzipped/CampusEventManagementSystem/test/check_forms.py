#!/usr/bin/env python3
"""Static NetBeans consistency check. Run from the project root:  python3 test/check_forms.py
For every src/**/X.form + X.java pair it verifies:
  1. each <EventHandler> in the .form has exactly one listener line in initComponents()
       component.addXListener(this::handler)   and exactly one method  handler(...) { //GEN-FIRST:event_handler
  2. each such listener line in initComponents() has a matching .form entry (no stale or hidden wiring)
  3. no handwritten code outside initComponents() adds a second listener of the same kind to a wired component
  4. every handler method marked GEN-FIRST has a .form entry (no orphan generated handler)
Exits 1 on any problem. It checks wiring text only; it does not open NetBeans or run the GUI."""
import glob, re, sys, xml.etree.ElementTree as ET

LISTENER = {'java.awt.event.ActionListener': 'addActionListener', 'javax.swing.event.ChangeListener': 'addChangeListener'}
problems, checked = [], 0

for form in sorted(glob.glob('src/**/*.form', recursive=True)):
    java = form[:-5] + '.java'
    src = open(java, encoding='utf-8').read().replace('\r\n', '\n')
    tree = ET.parse(form)
    wired = []                                   # (component, handler, adder)
    for el in tree.iter():
        name = el.get('name')
        if name is None: continue
        for ev in el.findall('./Events/EventHandler'):
            wired.append((name, ev.get('handler'), LISTENER.get(ev.get('listener'), '?' + ev.get('listener'))))
    b, e = src.find('//GEN-BEGIN:initComponents'), src.find('//GEN-END:initComponents')
    init = src[b:e]; rest = src[:b] + src[e:]
    lines = re.findall(r'(\w+)\.(add\w+Listener)\(this::(\w+)\)', init)
    checked += 1
    tag = form.split('/')[-1]
    for comp, h, adder in wired:
        if (comp, adder, h) not in lines: problems.append(f'{tag}: form wires {comp}.{h} but initComponents has no "{comp}.{adder}(this::{h})"')
        if lines.count((comp, adder, h)) > 1: problems.append(f'{tag}: duplicate listener line for {comp}.{h}')
        if len(re.findall(r'private void %s\(.*\{//GEN-FIRST:event_%s' % (h, h), src)) != 1:
            problems.append(f'{tag}: handler method {h} missing or duplicated (needs one //GEN-FIRST:event_{h})')
        for m in re.finditer(r'\b%s\.%s\(' % (comp, adder), rest):
            problems.append(f'{tag}: handwritten {comp}.{adder}(...) outside initComponents duplicates the form wiring of {h}')
    formset = {(c, a, h) for c, h, a in wired}
    for l in lines:
        if l not in formset: problems.append(f'{tag}: initComponents has {l[0]}.{l[1]}(this::{l[2]}) with no matching .form EventHandler')
    for h in re.findall(r'//GEN-FIRST:event_(\w+)', src):
        if h not in {x[1] for x in wired}: problems.append(f'{tag}: generated handler {h} has no .form entry')
    print(f'{tag:24} handlers in form: {len(wired):2}  listener lines: {len(lines):2}')

print('\n%d form(s) checked, %d problem(s)' % (checked, len(problems)))
for p in problems: print('PROBLEM:', p)
sys.exit(1 if problems else 0)
