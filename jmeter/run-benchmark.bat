del benchmark.jtl
jmeter -n -t benchmark.jmx -l benchmark.jtl

rmdir reports /s /q
jmeter -g benchmark.jtl -o reports -q user.properties