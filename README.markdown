XData (aka _Cross Data_) is a arbitrary data format, used to pass information around within a multi-layered application.

![https://raw.github.com/tooltwist/xdata/master/docs/images/basic-idea-1.jpeg](https://raw.github.com/tooltwist/xdata/master/docs/images/basic-idea-1.jpeg)

In an application architecture where a website is constructed by assembling components, the source of the data is not known at the time the UI components are written. In fact, a UI component may be used in many applications, with many data sources.

Similarly, the code to access data may not know the format expected by other code that consumes that data.

XData provides a format-neutral mechanism for accessing data in a variety of formats.

