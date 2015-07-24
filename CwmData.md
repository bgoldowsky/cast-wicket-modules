# Introduction #

This module includes several packages which define Application setup and initialization, administration pages, data classes, behaviours, criteria builders, custom Wicket components, data initializers,  data validators and data service classes.  This module includes a database structure useful in particular for educational web applications.

# Dependencies #
  * [cwm-base](CwmBase.md)
  * [cwm-drawtool](http://code.google.com/p/cwm-drawtool)
  * [cwm-audioapplet](CwmAudioapplet.md)
  * [cwm-components](CwmComponents.md)
  * Logback
  * Databinder
  * Hibernate
  * PostgreSQL
  * Visural Wicket
  * Apache Sanselan
  * EHCache
  * Wicketstuff TinyMCE
# Details #

The cwm package includes the Application class used to setup the application.  It initializes a set of pages as well as the persisted objects for cwm applications.  Session and Id management are also found in this package.

The admin package includes many useful pages for setting up and administering the cwm application.  Pages are available to create and list users as well as examine the event logs of user activities.

The data package includes all of the baseline persisted objects including:
  * User - which can have a Role such as student, teacher, researcher, admin
  * Site and class Period to organize groups of Users
  * LoginSession & Event to track usage of the application
  * Prompt and Response to allow for user input stored in the database
  * BinaryFileData for uploaded files, audio recordings, etc.

The init package provides the ability to create an administrator or load a list of default users from a data file.  It also closes any expired sessions.

The builders package provides a set of query builders for data related to Prompts, Responses, Login Sessions as well as Users.  The validator package provides data validations used for data integrity, such as enforcing uniqueness, checking types or correctness.  The service package consolidates database operations for Users, Sites, Responses, and other data objects.  The models package provides a variety of models for mapping the data object with the underlying data.  Databinder is currently used to manage most of these mappings.

The component package provides a variety of custom Wicket Components including links and buttons, dialog boxes and drop down choices.  It also includes panels to show and edit user responses of all types including plain text, formatted text, audio, SVG drawing, or uploaded files.  The behavior package includes behaviors such as auto saving or event logging.  The resource package includes classes for locating file or image resources.

The highlight package includes the java classes, HTML and javascript needed to add highlighting of text capability to a page or component.