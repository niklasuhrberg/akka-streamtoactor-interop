# Akka stream to actor interoperability sample applications

A sample application in three different versions showing different approaches to stream to actor interoperability in Akka streams.  

## Ask based 
A simple flow with actor interoperability using the ask pattern together with `ActorFlow.ask`

The code is prepared to enable simple modification to study different error scenarios. 
See comments in the code. 

## Stage actor based

A simple flow with actor interoperability using the stage actor based approach using `ActorRefBackpressureProcessFlowStage`
The code is prepared to enable simple modification to study different error scenarios and it also shows that the 
number of emittes elements is independent from the number of elements consumed by the stage actor based flow. 

## Stage actor with passthrough
This is not a standard passthrough flow in which every element out is accompanied with a passthough element but instead 
only the first element out corresponding to an element in has the passthrough, which is an Option.
The article explains one intended real world use case with Kafka commit information that this flow serves nicely.The state 
will typically only contain _one single element_ in the use case.

# Running instructions

Run the applications in your IDE or the following way with sbt. 

In an sbt shell: 

To run the ask based demo application issue:
`runMain com.triadicsystems.examples.askbased.MainAskBased`

To run the stageactor based demo application issue:
`run` 
or
`runMain com.triadicsystems.examples.stageactorbased.Main`


To run the passthrough enabled demo application issue:
`runMain com.triadicsystems.examples.passthrough.MainPassThrough`
