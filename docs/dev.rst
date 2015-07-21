Developing Mineotaur
====================

Software used to create Mineotaur
---------------------------------

Server side:
^^^^^^^^^^^^
* Programming language: `Java 8 <http://java.oracle.com/>`_
* Build system: `Apache Maven <http://maven.apache.org/>`_
* Database: `Neo4j <http://www.neo4j.com/>`_
* Web framework: `Spring Boot <http://projects.spring.io/spring-boot/>`_
* Template engine: `Thymeleaf <http://www.thymeleaf.org/>`_
* Test framework: `TestNG <http://www.testng.org/>`_
* Continous integration tool: `Travis CI <https://travis-ci.org/>`_
* Bytecode manipulation tool: `Javassist <http://jboss-javassist.github.io/javassist/>`_
* Command line parsing: `Apache Commons CLI <https://commons.apache.org/proper/commons-cli/>`_
* Math package: `Apache Commons Math <http://commons.apache.org/proper/commons-math//>`_

Client side:
^^^^^^^^^^^^
* Script and markup languages: Javascript, HTML 5, CSS
* Front-end framework: `Twitter Bootstrap <http://getbootstrap.com/>`_
* Visualization: `D3 <http://d3js.org/>`_
* HTML manipulation: `jQuery <https://jquery.com/>`_
* jQuery UI library: `jQuery-UI <https://jqueryui.com/>`_
* jQuery spinner: `spin.js <https://github.com/fgnass/spin.js>`_
* jQuery blockUI plugin: `jquery.blockui.js <http://malsup.com/jquery/block/>`_
* jQuery form plugin: `jquery.form.js <http://jquery.malsup.com/form/>`_
* jQuery history plugin: `history.js <https://github.com/browserstate/history.js>`_
* jQuery context menu: `jeegoocontext <http://www.tweego.nl/jeegoocontext>`_
* jQuery modal widget: `Magnific Popup <http://dimsemenov.com/plugins/magnific-popup/>`_
* jQuery multiselect widget: `jQuery UI MultiSelect widget <https://github.com/ehynds/jquery-ui-multiselect-widget/>`_
* jQuery modal widget: `Magnific Popup <http://dimsemenov.com/plugins/magnific-popup/>`_
* AMD framework: `RequireJS <http://requirejs.org/>`_
* General utility collection: `Underscore <http://underscorejs.org/>`_
* Math library: `numbers.js <https://github.com/numbers/numbers.js>`_
* Regression library: `regression.js <https://github.com/Tom-Alexander/regression-js>`_
* ZLib Javascript library: `Pako <https://github.com/nodeca/pako>`_

Architecture of Mineotaur
-------------------------

The Mineotaur web server can be accessed from both a web interface and programatically using REST. The web server handles the interaction with the graph database containing the HT/HCS data.

.. image:: /images/mineotaur_architecture_new.png
    :align: center

Server side architecture
^^^^^^^^^^^^^^^^^^^^^^^^

The web server if based on the Spring Model-View-Controller (MVC), using Thymeleaf as a template engine. The data is stored in the Neo4j graph database. A web client can access the content by making an HTTP request to the server, which will query the appropriate data from the database and render a web page from a Thymeleaf template.

.. image:: /images/mineotaur_server_new.png
    :align: center

Client side architecture
^^^^^^^^^^^^^^^^^^^^^^^^

On the client side, all interaction is done using a Javascript application. The application is modular, with different modules responsinble to handle events (Controller), carry data values (Context), manipulate web pages (UI), generate plots (Plot) and provide general functionalities (Utilities).

.. image:: /images/mineotaur_client_modules.png
    :align: center




