import React, { useState, useEffect, useRef } from 'react';
import { 
  MessageSquare, Settings, Plus, FileText, Database, 
  Upload, Send, RefreshCw, Edit3, Layers, Trash2, 
  BrainCircuit, ChevronRight, Search, Zap, FileCode2, 
  Copy, Download, Clock, BarChart3, TrendingUp, Merge2
} from 'lucide-react';

// --- MOCK DATA ---
const INITIAL_SCENARIO = "E-commerce Checkout Flow: Guest user purchasing a single physical item with a credit card.";
const MOCK_HISTORY = [
  { id: 1, title: "Login Page Validation Tests", date: "2 hrs ago" },
  { id: 2, title: "Shopping Cart Deduplication", date: "Yesterday" },
  { id: 3, title: "Stripe API Webhook Handlers", date: "3 days ago" },
];

const MOCK_TEST_CASES = [
  {
    id: 'tc-001',
    title: 'UAT-Repo-Assets, Checkout, Valid Credit Card, Verify Successful Payment',
    steps: ['Pre-condition: User is on checkout page', 'Enter valid card details', 'Click Pay button'],
    expectedResults: 'Payment should be processed successfully',
    status: 'Generated',
    version: 1,
    createdAt: '2 hrs ago'
  },
  {
    id: 'tc-002',
    title: 'UAT-Validation-Decision, Checkout, Invalid Expiry Date, Verify Error Message',
    steps: ['Enter card with expired date', 'Click Pay button'],
    expectedResults: 'System should display error message',
    status: 'Generated',
    version: 1,
    createdAt: '2 hrs ago'
  },
];

const MOCK_SIMILAR_CASES = [
  { title: 'Payment Processing - Card Validation', similarity: 0.92, conversationId: 'conv-004' },
  { title: 'Checkout Flow - Guest User', similarity: 0.88, conversationId: 'conv-002' },
  { title: 'Error Handling - Payment Gateway', similarity: 0.81, conversationId: 'conv-007' },
];

export default function ChatInterfaceJira() {
  // --- STATE ---
  const [activeTab, setActiveTab] = useState('chat');
  const [activeConversationId, setActiveConversationId] = useState(null);
  const [messages, setMessages] = useState([
    {
      id: 'system-1',
      role: 'assistant',
      content: "Hello. I am your QA & Testing Agent. I am connected to our pgvector knowledge base. What scenario are we working on today?",
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      isActionable: false
    }
  ]);
  const [inputValue, setInputValue] = useState('');
  const [isProcessing, setIsProcessing] = useState(false);
  const [processStep, setProcessStep] = useState('');
  const [leftSidebarOpen, setLeftSidebarOpen] = useState(true);
  const [rightSidebarOpen, setRightSidebarOpen] = useState(true);
  const [activeScenario, setActiveScenario] = useState(INITIAL_SCENARIO);
  const [uploadedFiles] = useState([{ name: "checkout_ui_v2.png", type: "image" }]);
  const [activeGuides] = useState(["UX_Guidelines_2026.pdf", "Payment_Gateway_Rules.md"]);
  const [vectorHits, setVectorHits] = useState(0);
  const [testCases] = useState(MOCK_TEST_CASES);
  const [selectedCaseId, setSelectedCaseId] = useState(null);

  const messagesEndRef = useRef(null);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, isProcessing]);

  // Initialize conversation on mount
  useEffect(() => {
    const initializeConversation = async () => {
      try {
        const response = await fetch('/api/v1/chat/start', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            scenario: activeScenario,
            guides: activeGuides,
            uploadedFiles: uploadedFiles
          })
        });

        if (response.ok) {
          const data = await response.json();
          setActiveConversationId(data.id);
        }
      } catch (error) {
        console.error('Error initializing conversation:', error);
      }
    };

    initializeConversation();
  }, []);

  // --- HANDLERS ---
  const handleSend = async (customText = null) => {
    const textToSend = customText || inputValue;
    if (!textToSend.trim()) return;

    const userMsgId = `user-${Date.now()}`;
    setMessages(prev => [...prev, {
      id: userMsgId,
      role: 'user',
      content: textToSend,
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    }]);
    setInputValue('');
    setIsProcessing(true);

    try {
      setProcessStep('1. Analyzing Scenario Text (Priority 1)...');
      await new Promise(r => setTimeout(r, 300));
      setProcessStep('2. Extracting Rules from User Guides (Priority 2)...');
      await new Promise(r => setTimeout(r, 300));
      setProcessStep('3. Scanning Uploaded Files & Screenshots (Priority 3)...');
      await new Promise(r => setTimeout(r, 300));
      setProcessStep('4. Generating text-embedding-3-small vector...');
      await new Promise(r => setTimeout(r, 300));
      setProcessStep('5. Querying pgvector for semantic matches (Priority 4)...');
      await new Promise(r => setTimeout(r, 500));
      setProcessStep('6. Generating response with assembled context...');

      // Call real API
      const response = await fetch('/api/v1/chat/message', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          conversationId: activeConversationId,
          message: textToSend,
          actionType: 'chat'
        })
      });

      if (!response.ok) throw new Error('API request failed');
      
      const data = await response.json();

      const aiMsgId = `ai-${Date.now()}`;
      setMessages(prev => [...prev, {
        id: aiMsgId,
        role: 'assistant',
        content: data.content,
        timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
        isActionable: true,
        ragStats: {
          vectorHits: data.vectorHits || 3,
          guidesUsed: data.guidesUsed || 2
        }
      }]);
      setVectorHits(data.vectorHits || 3);
    } catch (error) {
      console.error('Error sending message:', error);
      setMessages(prev => [...prev, {
        id: `ai-${Date.now()}`,
        role: 'assistant',
        content: 'Error: Failed to process request. Please try again.',
        timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
        isActionable: false
      }]);
    } finally {
      setIsProcessing(false);
      setProcessStep('');
    }
  };

  const handleAction = (actionType, msgId) => {
    let actionEndpoint = `/api/v1/chat/${activeConversationId}`;
    let prompt = "";
    
    switch(actionType) {
      case 'Regenerate': 
        actionEndpoint += '/regenerate';
        prompt = "Please regenerate those test cases with a different approach.";
        break;
      case 'Modify': 
        actionEndpoint += '/modify';
        prompt = "I need to modify the previous test cases.";
        break;
      case 'Add More': 
        actionEndpoint += '/add-more';
        prompt = "Please add more edge cases to that list.";
        break;
      case 'Merge': 
        actionEndpoint += '/merge';
        prompt = "Merge these new cases with the existing test suite.";
        break;
      case 'Delete': 
        setMessages(prev => prev.filter(m => m.id !== msgId));
        return;
      default: break;
    }
    
    // Call API endpoint
    fetch(actionEndpoint, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ message: prompt })
    })
    .then(res => res.json())
    .then(data => {
      handleSend(prompt);
    })
    .catch(error => console.error('Error invoking action:', error));
  };

  // --- TAB COMPONENTS ---

  const ChatTab = () => (
    <div className="flex-1 flex flex-col h-full relative bg-white">
      <div className="flex-1 overflow-y-auto p-6 pb-32">
        <div className="max-w-4xl mx-auto space-y-4">
          {messages.map((msg) => (
            <div key={msg.id} className={`flex gap-4 ${msg.role === 'user' ? 'justify-end' : 'justify-start'}`}>
              {msg.role !== 'user' && (
                <div className="w-8 h-8 rounded-full bg-blue-600 flex items-center justify-center flex-shrink-0">
                  <BrainCircuit size={16} className="text-white" />
                </div>
              )}
              
              <div className={`max-w-[85%] flex flex-col gap-1 ${msg.role === 'user' ? 'items-end' : 'items-start'}`}>
                <div className="flex items-center gap-2 px-1">
                  <span className="text-xs font-medium text-gray-600">{msg.role === 'user' ? 'You' : 'QA Agent'}</span>
                  <span className="text-[10px] text-gray-500">{msg.timestamp}</span>
                </div>
                
                <div className={`p-4 rounded-lg ${
                  msg.role === 'user' 
                    ? 'bg-blue-600 text-white shadow-sm' 
                    : 'bg-gray-100 text-gray-800 border border-gray-200'
                }`}>
                  <div className="text-sm leading-relaxed">{msg.content}</div>
                  
                  {msg.isActionable && (
                    <div className="flex flex-wrap gap-2 mt-3 pt-3 border-t border-gray-300">
                      {['Regenerate', 'Modify', 'Add More', 'Merge'].map((action) => (
                        <button 
                          key={action}
                          onClick={() => handleAction(action, msg.id)}
                          className="flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium bg-white hover:bg-gray-50 text-gray-700 rounded border border-gray-300 transition-colors"
                        >
                          {action === 'Regenerate' && <RefreshCw size={13} />}
                          {action === 'Modify' && <Edit3 size={13} />}
                          {action === 'Add More' && <Plus size={13} />}
                          {action === 'Merge' && <FileCode2 size={13} />}
                          {action}
                        </button>
                      ))}
                    </div>
                  )}
                </div>
              </div>
            </div>
          ))}
          
          {isProcessing && (
            <div className="flex gap-4 justify-start">
              <div className="w-8 h-8 rounded-full bg-blue-600 flex items-center justify-center flex-shrink-0">
                <RefreshCw size={14} className="text-white animate-spin" />
              </div>
              <div className="bg-gray-100 border border-blue-300 text-blue-700 p-3 rounded-lg text-sm font-medium flex items-center gap-3">
                <div className="flex gap-1">
                  <div className="w-1.5 h-1.5 bg-blue-600 rounded-full animate-bounce" style={{ animationDelay: '0ms' }}></div>
                  <div className="w-1.5 h-1.5 bg-blue-600 rounded-full animate-bounce" style={{ animationDelay: '150ms' }}></div>
                  <div className="w-1.5 h-1.5 bg-blue-600 rounded-full animate-bounce" style={{ animationDelay: '300ms' }}></div>
                </div>
                {processStep}
              </div>
            </div>
          )}
          <div ref={messagesEndRef} />
        </div>
      </div>

      <div className="absolute bottom-0 left-0 right-0 bg-gradient-to-t from-white via-white to-transparent pt-8 pb-6 px-6">
        <div className="max-w-4xl mx-auto">
          <div className="relative bg-white border border-gray-300 rounded-lg shadow-sm focus-within:border-blue-500 focus-within:ring-1 focus-within:ring-blue-500 transition-all">
            <textarea
              value={inputValue}
              onChange={(e) => setInputValue(e.target.value)}
              onKeyDown={(e) => { if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); handleSend(); } }}
              placeholder="Ask the agent to generate or modify test cases..."
              className="w-full bg-transparent text-gray-800 p-4 pr-14 outline-none resize-none min-h-[56px] max-h-40 text-sm placeholder-gray-500"
              rows="2"
            />
            <button 
              onClick={() => handleSend()}
              disabled={!inputValue.trim() || isProcessing}
              className="absolute right-3 bottom-3 p-2 bg-blue-600 hover:bg-blue-700 disabled:bg-gray-300 text-white rounded-lg transition-colors"
            >
              <Send size={16} />
            </button>
          </div>
        </div>
      </div>
    </div>
  );

  const TestCasesTab = () => (
    <div className="flex-1 overflow-y-auto bg-white p-6">
      <div className="max-w-6xl mx-auto">
        <div className="flex items-center justify-between mb-6">
          <div>
            <h2 className="text-2xl font-bold text-gray-900 mb-1">Generated Test Cases</h2>
            <p className="text-sm text-gray-600">Total: {testCases.length} cases</p>
          </div>
          <button 
            onClick={() => {
              fetch(`/api/chat/${activeConversationId}/export`)
                .then(res => res.text())
                .then(data => alert(data))
                .catch(error => alert('Export failed: ' + error.message));
            }}
            className="flex items-center gap-2 px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg font-medium"
          >
            <Download size={16} /> Export Excel
          </button>
        </div>

        <div className="space-y-3">
          {testCases.length > 0 ? testCases.map((tc) => (
            <div 
              key={tc.id}
              onClick={() => setSelectedCaseId(selectedCaseId === tc.id ? null : tc.id)}
              className="bg-white border border-gray-300 rounded-lg p-4 hover:border-blue-400 cursor-pointer transition-all hover:shadow-md"
            >
              <div className="flex items-start justify-between mb-3">
                <div className="flex-1">
                  <h3 className="font-medium text-gray-900 text-sm mb-2">{tc.title}</h3>
                  <div className="flex items-center gap-3 flex-wrap">
                    <span className="px-2 py-0.5 bg-green-100 text-green-800 text-xs rounded-full border border-green-300">
                      {tc.status || 'Generated'}
                    </span>
                    <span className="text-xs text-gray-600 flex items-center gap-1"><Clock size={12} /> {tc.createdAt}</span>
                    <span className="text-xs text-gray-600">v{tc.version}</span>
                  </div>
                </div>
                <button className="p-1.5 text-gray-600 hover:text-red-600 transition-colors bg-gray-100 rounded border border-gray-300">
                  <Trash2 size={14} />
                </button>
              </div>

              {selectedCaseId === tc.id && (
                <div className="mt-4 pt-4 border-t border-gray-200 space-y-3">
                  <div>
                    <h4 className="text-xs font-semibold text-gray-700 uppercase tracking-wider mb-2">Test Steps</h4>
                    <ol className="space-y-1 text-sm text-gray-700">
                      {(Array.isArray(tc.steps) ? tc.steps : []).map((step, idx) => (
                        <li key={idx} className="flex gap-3">
                          <span className="text-gray-500">{idx + 1}.</span>
                          <span>{step}</span>
                        </li>
                      ))}
                    </ol>
                  </div>
                  <div>
                    <h4 className="text-xs font-semibold text-gray-700 uppercase tracking-wider mb-2">Expected Result</h4>
                    <p className="text-sm text-gray-700">{tc.expectedResults}</p>
                  </div>
                </div>
              )}
            </div>
          )) : (
            <div className="text-center py-12 text-gray-500">
              <FileText size={48} className="mx-auto mb-4 opacity-30" />
              <p>No test cases generated yet. Start the chat to generate test cases.</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );

  const VectorKnowledgeTab = () => (
    <div className="flex-1 overflow-y-auto bg-white p-6">
      <div className="max-w-6xl mx-auto space-y-6">
        <h2 className="text-2xl font-bold text-gray-900">Vector Knowledge Base</h2>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div className="bg-blue-50 border border-blue-300 rounded-lg p-4">
            <div className="flex items-center justify-between mb-2">
              <span className="text-blue-900 text-xs font-semibold uppercase">Generated Embeddings</span>
              <Database size={18} className="text-blue-600" />
            </div>
            <div className="text-3xl font-bold text-blue-900">247</div>
            <p className="text-xs text-blue-700 mt-1">From 28 conversations</p>
          </div>

          <div className="bg-green-50 border border-green-300 rounded-lg p-4">
            <div className="flex items-center justify-between mb-2">
              <span className="text-green-900 text-xs font-semibold uppercase">Deduplication Matches</span>
              <Merge2 size={18} className="text-green-600" />
            </div>
            <div className="text-3xl font-bold text-green-900">12</div>
            <p className="text-xs text-green-700 mt-1">Similarity > 0.85</p>
          </div>

          <div className="bg-purple-50 border border-purple-300 rounded-lg p-4">
            <div className="flex items-center justify-between mb-2">
              <span className="text-purple-900 text-xs font-semibold uppercase">Last Query Time</span>
              <Zap size={18} className="text-purple-600" />
            </div>
            <div className="text-3xl font-bold text-purple-900">47ms</div>
            <p className="text-xs text-purple-700 mt-1">Average latency</p>
          </div>
        </div>

        <div className="bg-white border border-gray-300 rounded-lg p-6">
          <h3 className="text-lg font-semibold text-gray-900 mb-4 flex items-center gap-2">
            <Search size={18} className="text-blue-600" /> Semantically Similar Test Cases
          </h3>
          <div className="space-y-3">
            {MOCK_SIMILAR_CASES.map((item, idx) => (
              <div key={idx} className="flex items-center justify-between p-3 bg-gray-50 rounded-lg hover:bg-gray-100 cursor-pointer">
                <div className="flex-1">
                  <p className="text-sm font-medium text-gray-900">{item.title}</p>
                  <p className="text-xs text-gray-600">From Conversation {item.conversationId}</p>
                </div>
                <div className="flex items-center gap-2 ml-4">
                  <div className="w-20 h-1.5 bg-gray-300 rounded-full overflow-hidden">
                    <div 
                      className="h-full bg-blue-600 rounded-full"
                      style={{ width: `${item.similarity * 100}%` }}
                    ></div>
                  </div>
                  <span className="text-xs font-semibold text-blue-600 w-10 text-right">{(item.similarity * 100).toFixed(0)}%</span>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );

  const AnalysisTab = () => (
    <div className="flex-1 overflow-y-auto bg-white p-6">
      <div className="max-w-6xl mx-auto space-y-6">
        <h2 className="text-2xl font-bold text-gray-900">Conversation Analysis</h2>

        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          <div className="bg-white border border-gray-300 rounded-lg p-4 text-center">
            <TrendingUp size={24} className="text-blue-600 mx-auto mb-2" />
            <div className="text-2xl font-bold text-gray-900">8</div>
            <p className="text-xs text-gray-600 mt-1">Active Conversations</p>
          </div>
          <div className="bg-white border border-gray-300 rounded-lg p-4 text-center">
            <MessageSquare size={24} className="text-green-600 mx-auto mb-2" />
            <div className="text-2xl font-bold text-gray-900">124</div>
            <p className="text-xs text-gray-600 mt-1">Total Messages</p>
          </div>
          <div className="bg-white border border-gray-300 rounded-lg p-4 text-center">
            <FileText size={24} className="text-purple-600 mx-auto mb-2" />
            <div className="text-2xl font-bold text-gray-900">67</div>
            <p className="text-xs text-gray-600 mt-1">Test Cases</p>
          </div>
          <div className="bg-white border border-gray-300 rounded-lg p-4 text-center">
            <BarChart3 size={24} className="text-orange-600 mx-auto mb-2" />
            <div className="text-2xl font-bold text-gray-900">3.2h</div>
            <p className="text-xs text-gray-600 mt-1">Time Saved</p>
          </div>
        </div>

        <div className="bg-white border border-gray-300 rounded-lg p-6">
          <h3 className="text-lg font-semibold text-gray-900 mb-4">Recent Conversations</h3>
          <div className="space-y-2">
            {MOCK_HISTORY.map((conv) => (
              <div key={conv.id} className="flex items-center justify-between p-3 bg-gray-50 rounded hover:bg-gray-100 cursor-pointer">
                <div>
                  <p className="text-sm font-medium text-gray-900">{conv.title}</p>
                  <p className="text-xs text-gray-600">{conv.date}</p>
                </div>
                <ChevronRight size={16} className="text-gray-500" />
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );

  // --- RENDER ---
  return (
    <div className="flex h-screen w-full bg-gray-100 text-gray-900 font-sans overflow-hidden">
      
      {/* LEFT SIDEBAR */}
      <div className={`${leftSidebarOpen ? 'w-64' : 'w-0'} transition-all duration-300 flex-shrink-0 bg-white border-r border-gray-200 flex flex-col overflow-hidden`}>
        <div className="p-4 flex items-center justify-between border-b border-gray-200">
          <div className="flex items-center gap-2 font-semibold text-blue-600">
            <Database size={18} />
            <span>Chat History</span>
          </div>
        </div>
        <div className="p-3">
          <button className="w-full flex items-center gap-2 px-3 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-md transition-colors font-medium text-sm">
            <Plus size={16} /> New
          </button>
        </div>
        <div className="flex-1 overflow-y-auto p-2 space-y-1">
          <div className="px-3 py-2 text-xs font-semibold text-gray-600 uppercase tracking-wider">Recent</div>
          {MOCK_HISTORY.map(chat => (
            <button key={chat.id} className="w-full text-left flex items-start gap-3 px-3 py-2.5 hover:bg-gray-100 rounded transition-colors">
              <MessageSquare size={16} className="text-gray-600 mt-0.5" />
              <div className="flex-1 truncate">
                <div className="text-sm font-medium text-gray-800 truncate">{chat.title}</div>
                <div className="text-xs text-gray-600">{chat.date}</div>
              </div>
            </button>
          ))}
        </div>
        <div className="p-4 border-t border-gray-200 flex items-center gap-3 hover:bg-gray-100 cursor-pointer transition-colors">
          <Settings size={18} className="text-gray-600" />
          <span className="text-sm font-medium text-gray-800">Settings</span>
        </div>
      </div>

      {/* MAIN CONTENT */}
      <div className="flex-1 flex flex-col h-full relative bg-white">
        {/* Header with Tabs */}
        <header className="h-auto border-b border-gray-200 bg-white flex flex-col sticky top-0 z-10">
          <div className="h-14 flex items-center justify-between px-4 border-b border-gray-200">
            <div className="flex items-center gap-3">
              <button onClick={() => setLeftSidebarOpen(!leftSidebarOpen)} className="p-1.5 hover:bg-gray-100 rounded text-gray-700">
                <Layers size={20} />
              </button>
              <h1 className="font-semibold text-gray-900">Test Case Generator</h1>
              <span className="px-2 py-0.5 rounded-full bg-blue-100 text-blue-700 text-xs font-medium border border-blue-300 flex items-center gap-1">
                <Zap size={12} /> GPT-4o + pgvector
              </span>
            </div>
            <button onClick={() => setRightSidebarOpen(!rightSidebarOpen)} className="flex items-center gap-2 p-1.5 px-3 bg-gray-100 hover:bg-gray-200 rounded text-gray-700 text-sm font-medium border border-gray-300">
              <BrainCircuit size={16} /> RAG Context
            </button>
          </div>

          {/* TAB NAVIGATION */}
          <div className="flex items-center border-t border-gray-200 bg-gray-50">
            {[
              { id: 'chat', label: 'Chat', icon: MessageSquare },
              { id: 'testcases', label: 'Test Cases', icon: FileText },
              { id: 'vector', label: 'Vector Knowledge', icon: Database },
              { id: 'analysis', label: 'Analysis', icon: BarChart3 }
            ].map((tab) => {
              const Icon = tab.icon;
              return (
                <button
                  key={tab.id}
                  onClick={() => setActiveTab(tab.id)}
                  className={`flex items-center gap-2 px-4 py-3 text-sm font-medium transition-all border-b-2 ${
                    activeTab === tab.id
                      ? 'border-blue-600 text-blue-600 bg-blue-50'
                      : 'border-transparent text-gray-700 hover:text-gray-900 hover:bg-gray-100'
                  }`}
                >
                  <Icon size={16} />
                  {tab.label}
                </button>
              );
            })}
          </div>
        </header>

        {/* TAB CONTENT */}
        {activeTab === 'chat' && <ChatTab />}
        {activeTab === 'testcases' && <TestCasesTab />}
        {activeTab === 'vector' && <VectorKnowledgeTab />}
        {activeTab === 'analysis' && <AnalysisTab />}
      </div>

      {/* RIGHT SIDEBAR */}
      <div className={`${rightSidebarOpen ? 'w-80' : 'w-0'} transition-all duration-300 flex-shrink-0 bg-gray-50 border-l border-gray-200 flex flex-col overflow-hidden`}>
        <div className="p-4 border-b border-gray-200 bg-white">
          <div className="flex items-center gap-2 font-semibold text-blue-600 mb-1">
            <BrainCircuit size={18} />
            <span>RAG Context</span>
          </div>
          <p className="text-xs text-gray-600">Priority stack for generation</p>
        </div>

        <div className="flex-1 overflow-y-auto p-4 space-y-4">
          <div className="space-y-2">
            <div className="text-xs font-bold text-amber-700 uppercase tracking-wider bg-amber-100 px-2 py-1 rounded">
              Priority 1 - Scenario
            </div>
            <div className="p-3 bg-white border border-gray-200 rounded text-sm text-gray-700">
              {activeScenario}
            </div>
          </div>

          <div className="space-y-2">
            <div className="text-xs font-bold text-blue-700 uppercase tracking-wider bg-blue-100 px-2 py-1 rounded">
              Priority 2 - User Guides
            </div>
            <div className="space-y-1.5">
              {activeGuides.map((guide, idx) => (
                <div key={idx} className="flex items-center gap-2 p-2 bg-white border border-gray-200 rounded text-xs text-gray-700">
                  <FileText size={14} className="text-blue-600" />
                  {guide}
                </div>
              ))}
            </div>
          </div>

          <div className="space-y-2 pt-2 border-t border-gray-200">
            <div className="text-xs font-bold text-green-700 uppercase tracking-wider bg-green-100 px-2 py-1 rounded">
              Priority 4 - pgvector
            </div>
            <div className="p-3 bg-white border border-gray-200 rounded">
              <div className="flex items-center gap-3">
                <Database size={20} className="text-green-600" />
                <div>
                  <div className="text-sm font-semibold text-gray-900">Semantic Search</div>
                  <div className="text-xs text-gray-600">Last query: {vectorHits} matches</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

    </div>
  );
}
