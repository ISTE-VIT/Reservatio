from flask import Flask, render_template, request, redirect, session, url_for, jsonify
import pyqrcode
import pyrebase
from pyqrcode import QRCode
import firebase_admin
from firebase_admin import credentials, messaging
from dotenv import dotenv_values

config = dotenv_values(".env")
firebase_cred = credentials.Certificate("firebase.json")
firebase_app = firebase_admin.initialize_app(firebase_cred)

firebaseConfig = {'apiKey': config['API_KEY'],
                  'authDomain': config['AUTH_DOMAIN'],
                  'projectId': config['PROJECT_ID'],
                  'storageBucket': config['STORAGE_BUCKET'],
                  'messagingSenderId': config['MESSAGING_SENDER_ID'],
                  'appId': config['APP_ID'],
                  'measurementId': config['MEASUREMENT_ID'],
                  'databaseURL': config['DATABASE_URL']
                  }
firebase = pyrebase.initialize_app(firebaseConfig)
db = firebase.database()
auth = firebase.auth()

app = Flask(__name__)
app.config['SECRET_KEY'] = "c6e803cd18a8c528c161eb9fcf013245248506ffb540ff70"

def send_topic_push(topic, title, body):
    message = messaging.Message(notification=messaging.Notification(title=title,body=body),topic=topic)
    messaging.send(message)

@app.route("/", methods=["GET", "POST"])
def index():
    return render_template("index.html")

@app.route("/login", methods=["GET", "POST"])
def login_page():
    if request.method == "POST":
        email = request.form["email"]
        password = request.form["password"]
        try:
            auth.sign_in_with_email_and_password(email, password)
            session['logged_in'] = True
            session['email'] = email
            for user in db.child("users").get().each():
                if(db.child("users").child(user.key()).child("email").get().val() == session['email']):
                    session['name'] = db.child("users").child(user.key()).child("restaurant").get().val()
                    return redirect(url_for('service'))
        except:
            for user in db.child("users").get().each():
                if(db.child("users").child(user.key()).child("email").get().val() == email):
                    return render_template("login.html", error = "Invalid Password !!", success = False)
            return render_template("login.html", error = "Email not found !! Register to continue", success = False)
    else:
        if session.get('logged_in') is None:
            return render_template("login.html", error = False, success = False)
        else:
            return redirect(url_for('service'))
    
@app.route("/signup", methods=["GET", "POST"])
def signup_page():
    if request.method == "POST":
        email = request.form["email"]
        password = request.form["password"]
        restaurant = request.form["restaurant"]
        for user in db.get().each():
            if user.key() == restaurant:
                return render_template("register.html", error="Sorry!! Restaurant name already taken", success = False)
        if '@' in restaurant or '.' in restaurant:
            return render_template("register.html", error="Invalid Restaurant Name !!")
        try:
            auth.create_user_with_email_and_password(email, password)
            db.child("users").push({"email":email,"restaurant" : restaurant})
            d = {restaurant: {"dummy": -2}}
            db.update(d)
            return render_template("login.html", success="Account Created Successfully !! Login to Proceed", error = False ) #change variable to status
        except:
            return render_template("register.html", error="Account already exists !!", success = False ) #change variable to status
    else:
        return render_template("register.html",error = False, success = False)
    
@app.route("/logout", methods=["GET", "POST"])
def logout():
    session['logged_in'] = None
    session['email'] = None
    return render_template("login.html", error="Logged Out !!", success = False)

@app.route("/service", methods=["GET", "POST"])
def service():
    if session['logged_in'] is None:
        return redirect(url_for('index'))
    else:
        url = pyqrcode.create(session['name'])
        image_as_str = url.png_as_base64_str(scale=5)
        return render_template("service.html", name = session['name'], url=image_as_str)

@app.route("/contact/", methods=['POST', 'GET'])
def contact():
    return render_template("contact.html")


@app.route("/getdata", methods=['POST', 'GET'])
def getdata():
    data_file = db.child(session['name']).get()
    count = 0
    for details in data_file.each():
        if (details.val() >= 1):
            count = count + 1
    if (count >= 1):
        disp = str(count)
    else:
        disp = "No Customers"
    return jsonify(result=disp)


@app.route("/get_current_user", methods=['POST', 'GET'])
def getuser():
    data_file = db.child(session['name']).get()
    for details in data_file.each():
        if (details.val() == 1):
            return jsonify(result="Next User : " + details.key().split("-")[0])
    return jsonify(result="")


@app.route("/getdata2", methods=['POST', 'GET'])
def getdata2():
    data_file = db.child(session['name']).get()
    d = dict()
    for details in data_file.each():
        if(details.val() == 1):
            send_topic_push(str(details.key())[str(details.key()).index('-')+1:], "It's Your Turn", "Please proceed to the restaurant")
        if(details.val() == 2):
            send_topic_push(str(details.key())[str(details.key()).index('-')+1:], "Your Turn is Coming Soon", "Please be ready to proceed to the restaurant")
        if(details.val() == 0):
            db.child(session['name']).child(details.key()).remove()
        elif (details.key() != "dummy"):
            d[details.key()] = details.val()-1
            if d[details.key()] == 0:
                db.child(session['name']).update(d)
                db.child(session['name']).child(details.key()).remove()
                d.pop(details.key())
    db.child(session['name']).update(d)

    return jsonify(result=d)

if __name__ == "__main__":
    app.run()
