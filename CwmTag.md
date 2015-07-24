# Introduction #

This module Includes the database classes and wicket components necessary for basic tagging functionality including:

> defining taggable objects (can be any database-persisted classes)
> displaying current tags for an object and controls to change them
> displaying tag lists and clouds, showing frequency

# Dependencies #
  * [cwm-data](CwmData.md)

# Details #

The basic objects for tags are Tag and Tagging.  A Tag represents a keyword that can be used to group items.  A Tagging is used to connect a specific object with a specific Tag.

The TagService class defines database operations, and maintains the list of types of objects that can be tagged.  This specification should occur at application start up.

A list of default Tags may also be added.  These default Tags are displayed to all users as a default set in the tagging panel.  No additional global tagging features are implemented at this time.

A user’s current tags may be viewed in both a cloud and a list format.  In the cloud format font sizing (or other attributes of visual weight) is based on the count of taggings using that tag (class attributes are applied by TagCloudPanel; their styling is specified in your site’s CSS.  By default, there are four different classes used).  In the list format, Tags instead are shown with a numerical label indicating the frequency of use.  Tags may be sorted by the frequency they occur or alphabetically.  Normally tags are expected to link to a list of the resources that are tagged; this is enabled by providing a class that implements !ITagLinkBuilder.

A TagPanel is available which will list all Tags currently Tagging a given object.  The panel enables the addition or removal of Taggings as well as creation of new Tags.

Tags are displayed using a TagLabel class.  This has the feature that asterisks are displayed as nice looking stars, so you can use tagging to flexibly implement star ratings like: ★★★★.