# Introduction #

This module defines the basic structure of a glossary - terms, definitions, alternate forms of terms, etc.  It builds on cwm-xml to enable interpreting an XML file as a list of glossary terms and their definitions (which can contain any amount or type of content).

The Glossary can be queried, listed, etc. There is also a Glossary transformer which takes (XML) text, finds glossary terms that are in it, and marks up the first instance of each term (per page or section) with a link to its definition.  How these links appear is defined by subsequent processing  - e.g., the definition could be presented inline, as a tooltip, in a popup window, or in some other way that you devise.

# Dependencies #
  * [cwm-xml](CwmXml.md)

# Details #

The Glossary is a class that serves as a collection of glossary entries.  Glossary entries may be created from an XML file that is parsed and transformed into the Glossary class (GlossaryXmlDocumentObserver).  The Glossary class may also be mapped to Glossary Entries from an alternative data store.  Each glossary entry is stored as an element in the Glossary.  The GlossaryEntry class represents an individual word or term.  Each glossary entry may have zero to many alternate forms.  For example, the word “foot” may also specify the alternate form “feet.”  Each glossary may have a short and a full definition.

The Glossary Transformer will examine XML data for possible glossary word occurrences.  Elements that should be scanned as well as elements or classes that should be ignored when identifying glossary words are defined in the Glossary Transformer.  Once a glossary word occurrence has been found, it is wrapped with a glossary indicator element.  This would normally be turned into a link or active element in a later XML-to-HTML transformer. In general, only one occurrence of a glossary indicator link is added to each XML section that is run through the transformer.

A template panel for a basic glossary page is available in this package.  It provides a container with letter navigation and word selection.  The letter navigation also has special handling for words that may not start with a letter.