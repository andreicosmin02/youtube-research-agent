import tkinter as tk
from tkinter import scrolledtext, messagebox, simpledialog
import requests
import json
from datetime import datetime

class YouTubeResearchAgent:
    def __init__(self, root):
        self.root = root
        self.root.title("YouTube Research Agent")
        self.root.geometry("900x700")
        
        self.api_url = "http://localhost:8080/api"
        self.token = None
        self.conversation_id = None
        self.username = None
        
        self.setup_ui()
    
    def setup_ui(self):
        """Setup the UI layout"""
        # Top frame for auth
        auth_frame = tk.Frame(self.root, bg="#f0f0f0", pady=10)
        auth_frame.pack(fill=tk.X, padx=10)
        
        # Username and password inputs
        tk.Label(auth_frame, text="Username:", bg="#f0f0f0").pack(side=tk.LEFT, padx=5)
        self.username_entry = tk.Entry(auth_frame, width=15)
        self.username_entry.pack(side=tk.LEFT, padx=5)
        
        tk.Label(auth_frame, text="Password:", bg="#f0f0f0").pack(side=tk.LEFT, padx=5)
        self.password_entry = tk.Entry(auth_frame, width=15, show="*")
        self.password_entry.pack(side=tk.LEFT, padx=5)
        
        tk.Button(auth_frame, text="Register", command=self.register_user, bg="#4CAF50", fg="white").pack(side=tk.LEFT, padx=5)
        tk.Button(auth_frame, text="Login", command=self.login_user, bg="#2196F3", fg="white").pack(side=tk.LEFT, padx=5)
        
        # Status label
        self.status_label = tk.Label(self.root, text="Not logged in", fg="red", bg="#f0f0f0", pady=5)
        self.status_label.pack(fill=tk.X, padx=10)
        
        # Conversation frame
        conv_frame = tk.Frame(self.root)
        conv_frame.pack(fill=tk.X, padx=10, pady=10)
        
        tk.Label(conv_frame, text="Conversation Title:", font=("Arial", 10, "bold")).pack(side=tk.LEFT, padx=5)
        self.conv_title_entry = tk.Entry(conv_frame, width=40)
        self.conv_title_entry.pack(side=tk.LEFT, padx=5)
        self.conv_title_entry.insert(0, "New Conversation")
        
        tk.Button(conv_frame, text="Create Conversation", command=self.create_conversation, bg="#FF9800", fg="white").pack(side=tk.LEFT, padx=5)
        
        # Chat display
        tk.Label(self.root, text="Chat History:", font=("Arial", 10, "bold")).pack(anchor=tk.W, padx=10)
        
        self.chat_display = scrolledtext.ScrolledText(
            self.root, 
            height=20, 
            width=100,
            state=tk.DISABLED,
            wrap=tk.WORD,
            bg="#ffffff",
            font=("Arial", 10)
        )
        self.chat_display.pack(padx=10, pady=5, fill=tk.BOTH, expand=True)
        
        # Configure text tags for styling
        self.chat_display.tag_config("user", foreground="#2196F3", font=("Arial", 10, "bold"))
        self.chat_display.tag_config("assistant", foreground="#4CAF50", font=("Arial", 10, "bold"))
        self.chat_display.tag_config("timestamp", foreground="#999999", font=("Arial", 8))
        
        # Input frame
        input_frame = tk.Frame(self.root)
        input_frame.pack(fill=tk.X, padx=10, pady=10)
        
        tk.Label(input_frame, text="Your Message:", font=("Arial", 10, "bold")).pack(anchor=tk.W)
        
        self.message_input = tk.Entry(input_frame, width=100, font=("Arial", 10))
        self.message_input.pack(fill=tk.X, pady=5)
        self.message_input.bind("<Return>", lambda e: self.send_message())
        
        tk.Button(input_frame, text="Send Message", command=self.send_message, bg="#2196F3", fg="white", width=20).pack(anchor=tk.E, pady=5)
    
    def register_user(self):
        """Register a new user"""
        username = self.username_entry.get()
        password = self.password_entry.get()
        
        if not username or not password:
            messagebox.showerror("Error", "Please enter username and password")
            return
        
        try:
            response = requests.post(
                f"{self.api_url}/users/register",
                json={"username": username, "password": password}
            )
            
            if response.status_code == 200:
                messagebox.showinfo("Success", f"User {username} registered successfully!")
                self.password_entry.delete(0, tk.END)
            else:
                error = response.json().get("message", response.text)
                messagebox.showerror("Error", f"Registration failed: {error}")
        except Exception as e:
            messagebox.showerror("Error", f"Connection error: {str(e)}")
    
    def login_user(self):
        """Login user and get JWT token"""
        username = self.username_entry.get()
        password = self.password_entry.get()
        
        if not username or not password:
            messagebox.showerror("Error", "Please enter username and password")
            return
        
        try:
            response = requests.post(
                f"{self.api_url}/users/login",
                json={"username": username, "password": password}
            )
            
            if response.status_code == 200:
                data = response.json()
                self.token = data["token"]
                self.username = username
                self.status_label.config(text=f"Logged in as: {username}", fg="green")
                messagebox.showinfo("Success", f"Logged in as {username}!")
                self.password_entry.delete(0, tk.END)
            else:
                messagebox.showerror("Error", "Invalid username or password")
        except Exception as e:
            messagebox.showerror("Error", f"Connection error: {str(e)}")
    
    def create_conversation(self):
        """Create a new conversation"""
        if not self.token:
            messagebox.showerror("Error", "Please login first")
            return
        
        title = self.conv_title_entry.get()
        
        try:
            response = requests.post(
                f"{self.api_url}/conversations",
                headers={"Authorization": f"Bearer {self.token}"},
                json={"title": title}
            )
            
            if response.status_code == 201:
                data = response.json()
                self.conversation_id = data["id"]
                self.chat_display.config(state=tk.NORMAL)
                self.chat_display.delete(1.0, tk.END)
                self.chat_display.insert(tk.END, f"=== Conversation: {title} ===\n\n", "timestamp")
                self.chat_display.config(state=tk.DISABLED)
                messagebox.showinfo("Success", f"Conversation created: {title}")
            else:
                messagebox.showerror("Error", "Failed to create conversation")
        except Exception as e:
            messagebox.showerror("Error", f"Connection error: {str(e)}")
    
    def send_message(self):
        """Send a message to the conversation"""
        if not self.token:
            messagebox.showerror("Error", "Please login first")
            return
        
        if not self.conversation_id:
            messagebox.showerror("Error", "Please create a conversation first")
            return
        
        message = self.message_input.get().strip()
        
        if not message:
            return
        
        try:
            # Display user message
            self.chat_display.config(state=tk.NORMAL)
            timestamp = datetime.now().strftime("%H:%M:%S")
            self.chat_display.insert(tk.END, f"[{timestamp}] ", "timestamp")
            self.chat_display.insert(tk.END, f"You: {message}\n", "user")
            self.chat_display.see(tk.END)
            self.chat_display.config(state=tk.DISABLED)
            
            # Clear input
            self.message_input.delete(0, tk.END)
            
            # Send to server
            response = requests.post(
                f"{self.api_url}/conversations/{self.conversation_id}/send-message",
                headers={"Authorization": f"Bearer {self.token}"},
                json={"message": message}
            )
            
            if response.status_code == 200:
                data = response.json()
                assistant_response = data.get("response", "No response")
                
                # Display assistant response
                self.chat_display.config(state=tk.NORMAL)
                timestamp = datetime.now().strftime("%H:%M:%S")
                self.chat_display.insert(tk.END, f"[{timestamp}] ", "timestamp")
                self.chat_display.insert(tk.END, f"Assistant: {assistant_response}\n\n", "assistant")
                self.chat_display.see(tk.END)
                self.chat_display.config(state=tk.DISABLED)
            else:
                error = response.json().get("message", response.text)
                messagebox.showerror("Error", f"Failed to send message: {error}")
        except Exception as e:
            messagebox.showerror("Error", f"Connection error: {str(e)}")

if __name__ == "__main__":
    root = tk.Tk()
    app = YouTubeResearchAgent(root)
    root.mainloop()