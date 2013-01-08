## Quick Links
[Wiki](https://github.com/tooltwist/xdata/wiki)  
[Group](https://groups.google.com/forum/?fromgroups#!forum/xdata-tooltwist)  

## What is XData?
XData (aka _Cross Data_) is an "arbitrary data format" library for Java, allowing data to be passed around within a multi-layered Java application without concern for whether it's in XML, JSON, or some other format.

![https://raw.github.com/tooltwist/xdata/master/docs/images/basic-idea-1.jpeg](https://raw.github.com/tooltwist/xdata/master/docs/images/basic-idea-1.jpeg)

## Why do I want it?
In an application architecture where a website is constructed by assembling components, the source of the data is not known at the time the UI components are written. A UI component may be used in many applications with many data sources, and there's no way of knowing the format of those various data sources at the time the UI component is written. XData allows such components to be developed without consideration for whether data will be provided as XML, JSON, or some other format.

Similarly, the code to access data may not know the format expected by other code that consumes that data.

XData provides a format-neutral mechanism for passing around data, but a consistent API for accessing data. For example, the following XPath-like code can be used to access both XML and JSON data.

    XD data = getCountryData();
    for (XD state : data.select("/*/state")) {
        System.out.println("State is " + state.getString("name"));
    }

XData also allows access to object representations of data (e.g. XML as DOM), with automatic conversions transparently performed as required.

## Mapping
In many cases, the source of data may use different field names to the consumer of that data, even though the values are acceptable. For example, a graph UI component might display X versus Y, but a data source provides _lengthOfHair_ versus _lengthOfMoustache_. XData allows a simple mapping of field names between the source and the time the data is accessed, including the ability to access nested data.

    person/lengthOfHair -> graph/x
    person/lower_face_information/lengthOfMoustach -> graph/y


##Fast Parser
XData also provides _fast parsing_ to give improved performance in cases where the data is large. Typical XML and JSON parsers convert input data from string format into an object representation of the data (e.g. DOM). While this provides flexibility, it can result in a lot of instantiation and garbage collection overhead, creating objects for every element in the document, when typically only part of the data may be required.

XData's _fast parsing_ creates an index over the string representation of the document which is then used to cherry pick specific data values. In some applications, such as accessing large data sets over a Restful interface, this approach can be about ten times faster than common parsers, once garbage collection cost is taken into account. Take a look at the benchmarks section in the wiki for an understanding of where this is appropriate.


--
