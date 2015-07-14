Installation
============

<h3>Mineotaur installation:</h3>

1.	Download the latest jar file from http://www.mineotaur.org.
2.	Create a property file, a data file and a label file (see documentation and example input data)
3.	Start the data import with the following command:
java -jar &lt;path_to_jar file&gt; -import mineotaur.input chia_sample.tsv chia_labels.ts
4.	After the database creation is completed you can start your Mineotaur instance with the following command:
java –jar &lt;path_to_jar file&gt; -start <instance_name>
5.	You can start querying at http://127.0.0.1:8080 in your browser.