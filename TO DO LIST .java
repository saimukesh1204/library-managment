import React, { useState, useEffect, useCallback } from 'react';
import { initializeApp } from 'firebase/app';
import { 
    getAuth, 
    signInAnonymously, 
    signInWithCustomToken, 
    onAuthStateChanged 
} from 'firebase/auth';
import { 
    getFirestore, 
    collection, 
    query, 
    onSnapshot, 
    addDoc, 
    updateDoc, 
    deleteDoc, 
    doc 
} from 'firebase/firestore';

const Plus = (props) => (<svg {...props} xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M5 12h14M12 5v14"/></svg>);
const Trash2 = (props) => (<svg {...props} xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M3 6h18"/><path d="M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6"/><path d="M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2"/><line x1="10" x2="10" y1="11" y2="17"/><line x1="14" x2="14" y1="11" y2="17"/></svg>);
const Check = (props) => (<svg {...props} xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polyline points="20 6 9 17 4 12"/></svg>);
const Database = (props) => (<svg {...props} xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><ellipse cx="12" cy="5" rx="9" ry="3"/><path d="M3 5v14c0 1.2 4 2 9 2s9-.8 9-2V5"/></svg>);

const SIMULATED_API_LATENCY = 500;
const apiEndpoint = "https://javataskapi.com/v1/tasks"; 

const fetchTasksFromApi = async (userId, firestoreTasks) => {
    return new Promise(resolve => {
        setTimeout(() => {
            const apiFormattedTasks = firestoreTasks.map(task => ({
                id: task.id,
                text: task.text,
                status: task.completed ? 'DONE' : 'PENDING',
                ownerId: task.userId,
                timestamp: task.createdAt,
            }));
            resolve(apiFormattedTasks);
        }, SIMULATED_API_LATENCY);
    });
};

const App = () => {
    const [tasks, setTasks] = useState([]);
    const [newTaskText, setNewTaskText] = useState('');
    const [userId, setUserId] = useState(null);
    const [db, setDb] = useState(null);
    const [isAuthReady, setIsAuthReady] = useState(false);
    const [loading, setLoading] = useState(true);
    const [dataMode, setDataMode] = useState('FIREBASE');
    
    const appId = typeof __app_id !== 'undefined' ? __app_id : 'default-app-id';
    const firebaseConfig = typeof __firebase_config !== 'undefined' ? JSON.parse(__firebase_config) : null;
    const initialAuthToken = typeof __initial_auth_token !== 'undefined' ? __initial_auth_token : null;

    useEffect(() => {
        if (!firebaseConfig) {
            setLoading(false);
            return;
        }

        const app = initializeApp(firebaseConfig);
        const firestore = getFirestore(app);
        const auth = getAuth(app);
        setDb(firestore);

        const unsubscribe = onAuthStateChanged(auth, async (user) => {
            if (user) {
                setUserId(user.uid);
            } else {
                setUserId(null);
            }
            setIsAuthReady(true);
            setLoading(false);
        });

        const authenticate = async () => {
            try {
                if (initialAuthToken) {
                    await signInWithCustomToken(auth, initialAuthToken);
                } else {
                    await signInAnonymously(auth);
                }
            } catch (error) {}
        };

        authenticate();
        return () => unsubscribe();
    }, []);

    const fetchTasks = useCallback((dbInstance, currentUserId) => {
        if (!isAuthReady || !dbInstance || !currentUserId) return () => {};

        const tasksCollectionRef = collection(dbInstance, `artifacts/${appId}/public/data/tasks`);
        const q = query(tasksCollectionRef);

        const unsubscribe = onSnapshot(q, async (snapshot) => {
            const firestoreTasks = snapshot.docs.map(doc => ({
                id: doc.id,
                ...doc.data()
            })).sort((a, b) => (a.createdAt || 0) - (b.createdAt || 0)); 

            if (dataMode === 'FIREBASE') {
                setTasks(firestoreTasks);
            } else if (dataMode === 'API') {
                const apiData = await fetchTasksFromApi(currentUserId, firestoreTasks);
                const reactFormattedTasks = apiData.map(task => ({
                    id: task.id,
                    text: task.text,
                    completed: task.status === 'DONE',
                    userId: task.ownerId,
                    createdAt: task.timestamp,
                }));
                setTasks(reactFormattedTasks);
            }

        });

        return () => unsubscribe();
    }, [isAuthReady, appId, dataMode]);

    useEffect(() => {
        return fetchTasks(db, userId);
    }, [db, userId, fetchTasks]);

    const addTask = async (e) => {
        e.preventDefault();
        const text = newTaskText.trim();
        if (text === '' || !db || !userId) return;
        try {
            await addDoc(collection(db, `artifacts/${appId}/public/data/tasks`), {
                text: text,
                completed: false,
                userId: userId, 
                createdAt: Date.now()
            });
            setNewTaskText('');
        } catch (error) {}
    };

    const toggleTask = async (id, completed) => {
        if (!db) return;
        try {
            const taskDocRef = doc(db, `artifacts/${appId}/public/data/tasks`, id);
            await updateDoc(taskDocRef, {
                completed: !completed
            });
        } catch (error) {}
    };

    const deleteTask = async (id) => {
        if (!db) return;
        try {
            const taskDocRef = doc(db, `artifacts/${appId}/public/data/tasks`, id);
            await deleteDoc(taskDocRef);
        } catch (error) {}
    };

    if (loading) {
        return (
            <div className="flex items-center justify-center min-h-screen bg-indigo-50">
                <p className="text-xl text-indigo-700 font-semibold">Loading and authenticating...</p>
            </div>
        );
    }

    return (
        <div className="min-h-screen bg-indigo-50 p-4 sm:p-8">
            <div className="max-w-xl mx-auto bg-white shadow-2xl rounded-xl p-6 md:p-8">
                
                <header className="mb-6 border-b pb-4">
                    <h1 className="text-3xl font-extrabold text-indigo-700">
                        Shared To-Do List
                    </h1>
                    <div className="mt-2 flex justify-between items-center">
                        <p className="text-sm text-gray-500">
                            Tasks update in real-time for all users.
                        </p>
                        <div className="flex items-center gap-2">
                            <span className="text-sm font-medium text-gray-700">Data Source:</span>
                            <button
                                onClick={() => setDataMode(dataMode === 'FIREBASE' ? 'API' : 'FIREBASE')}
                                className={`px-3 py-1 text-xs font-bold rounded-full transition duration-150 shadow-md flex items-center ${
                                    dataMode === 'FIREBASE' 
                                        ? 'bg-blue-500 text-white hover:bg-blue-600'
                                        : 'bg-orange-500 text-white hover:bg-orange-600'
                                }`}
                            >
                                <Database className="w-4 h-4 mr-1"/>
                                {dataMode === 'FIREBASE' ? 'Direct (Firestore)' : 'Simulated API (Java)'}
                            </button>
                        </div>
                    </div>
                    <div className="mt-2 p-2 bg-indigo-100 rounded-lg text-xs font-mono text-indigo-800 break-words">
                        Your ID: <span className="font-bold">{userId || 'N/A'}</span>
                    </div>
                </header>

                <form onSubmit={addTask} className="flex gap-3 mb-8">
                    <input
                        type="text"
                        value={newTaskText}
                        onChange={(e) => setNewTaskText(e.target.value)}
                        placeholder="Enter a new task..."
                        className="flex-grow p-3 border border-indigo-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 transition duration-150"
                        disabled={!userId}
                        required
                    />
                    <button
                        type="submit"
                        className="bg-indigo-600 hover:bg-indigo-700 text-white p-3 rounded-lg font-semibold shadow-md disabled:bg-indigo-400 flex items-center justify-center transition duration-150"
                        disabled={!userId || newTaskText.trim() === ''}
                    >
                        <Plus className="w-5 h-5 mr-1" /> Add Task
                    </button>
                </form>

                <ul className="space-y-3">
                    {tasks.length === 0 ? (
                        <li className="text-center text-gray-500 p-4 border border-dashed rounded-lg">
                            No tasks yet! Add one above.
                        </li>
                    ) : (
                        tasks.map((task) => (
                            <li
                                key={task.id}
                                className={`flex items-center justify-between p-4 rounded-lg shadow-sm transition duration-200 ${
                                    task.completed 
                                        ? 'bg-green-50 border-l-4 border-green-500' 
                                        : 'bg-white border-l-4 border-indigo-500'
                                }`}
                            >
                                <div className="flex items-center flex-grow cursor-pointer" onClick={() => toggleTask(task.id, task.completed)}>
                                    <div className={`w-6 h-6 rounded-full border-2 mr-4 flex items-center justify-center transition duration-200 ${
                                        task.completed 
                                            ? 'bg-green-500 border-green-600' 
                                            : 'border-indigo-400 hover:bg-indigo-100'
                                    }`}>
                                        {task.completed && <Check className="w-4 h-4 text-white" />}
                                    </div>
                                    <span className={`text-gray-800 text-lg ${task.completed ? 'line-through text-gray-400' : ''}`}>
                                        {task.text}
                                    </span>
                                </div>

                                <div className="flex items-center gap-2">
                                    <span title={`Created by: ${task.userId}`} className="text-xs text-gray-400 hidden sm:block">
                                        ({task.userId.substring(0, 4)}...)
                                    </span>
                                    <button
                                        onClick={() => deleteTask(task.id)}
                                        className="text-red-500 hover:text-red-700 p-2 rounded-full transition duration-150"
                                        title="Delete Task"
                                    >
                                        <Trash2 className="w-5 h-5" />
                                    </button>
                                </div>
                            </li>
                        ))
                    )}
                </ul>
            </div>
        </div>
    );
};

export default App;
