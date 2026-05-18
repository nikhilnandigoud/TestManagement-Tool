package com.qaautomation.agents;

/**
 * Base interface for all agents in the QA Automation Platform.
 * Each agent is responsible for a single, well-defined task.
 */
public interface Agent {
    /**
     * Executes the agent's primary responsibility.
     * @return true if execution was successful, false otherwise
     */
    boolean execute();
    
    /**
     * Gets a description of the agent's responsibility.
     */
    String getDescription();
    
    /**
     * Validates if the agent can handle the given input.
     */
    boolean canHandle(Object input);
}
