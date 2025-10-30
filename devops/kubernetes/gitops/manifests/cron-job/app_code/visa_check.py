import os
import psycopg2
from sendgrid import SendGridAPIClient
from sendgrid.helpers.mail import Mail

# Set default environment variables for testing
os.environ.setdefault("DB_HOST", "localhost")
os.environ.setdefault("DB_PORT", "5432")
os.environ.setdefault("DB_NAME", "test_db")
os.environ.setdefault("DB_USER", "test_user")
os.environ.setdefault("DB_PASSWORD", "test_password")
os.environ.setdefault("SENDGRID_API_KEY", "test_sendgrid_key")
os.environ.setdefault("SENDGRID_FROM_EMAIL", "from@example.com")
os.environ.setdefault("SENDGRID_TO_EMAILS", "to@example.com")

def get_db_connection():
    try:
        return psycopg2.connect(
            host=os.environ["DB_HOST"],
            port=int(os.environ.get("DB_PORT", 5432)),
            dbname=os.environ["DB_NAME"],
            user=os.environ["DB_USER"],
            password=os.environ["DB_PASSWORD"]
        )
    except Exception as e:
        print(f"Error connecting to DB: {e}")
        raise

def fetch_expiring_visas():
    conn = get_db_connection()
    cur = conn.cursor()
    cur.execute("""
    SELECT *
    FROM visa
    JOIN person ON visa.person_id = person.id
    WHERE visa.visa_expiry_date <= NOW() + INTERVAL '30 days'
      AND visa.visa_expiry_date >= NOW();
    """)
    rows = cur.fetchall()
    column_names = [desc[0] for desc in cur.description]
    cur.close()
    conn.close()
    return rows, column_names

def generate_html_table(columns, rows):
    html_table = "<table border='1' cellpadding='5' cellspacing='0' style='border-collapse: collapse;'>"
    html_table += "<thead><tr>" + "".join(f"<th>{col}</th>" for col in columns) + "</tr></thead><tbody>"
    for row in rows:
        html_table += "<tr>" + "".join(f"<td>{cell}</td>" for cell in row) + "</tr>"
    html_table += "</tbody></table>"
    return html_table

def send_email_alert(from_email, to_emails, html_content, sg_api_key):
    if not html_content:
        return False
    message = Mail(
        from_email=from_email,
        to_emails=to_emails,
        subject="Visa Expiry Alert",
        plain_text_content="The following visas are expiring in the next 30 days. Please view the HTML version of this email.",
        html_content=html_content
    )
    try:
        sg = SendGridAPIClient(sg_api_key)
        response = sg.send(message)
        return response.status_code == 202
    except Exception as e:
        print(f"Error sending email: {e}")
        return False

def main():
    rows, columns = fetch_expiring_visas()
    if rows:
        html_table = generate_html_table(columns, rows)
        html_content = f"<p>The following visas are expiring in the next 30 days:</p>{html_table}"
        success = send_email_alert(
            from_email=os.environ["SENDGRID_FROM_EMAIL"],
            to_emails=os.environ["SENDGRID_TO_EMAILS"].split(","),
            html_content=html_content,
            sg_api_key=os.environ["SENDGRID_API_KEY"]
        )
        print("Email sent successfully!" if success else "Failed to send email.")

if __name__ == "__main__":
    main()
