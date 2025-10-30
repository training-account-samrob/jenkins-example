import unittest
from unittest.mock import patch, MagicMock
from visa_check import generate_html_table, send_email_alert, fetch_expiring_visas

def test_generate_html_table():
    columns = ["id", "name", "visa_expiry_date"]
    rows = [
        (1, "Alice", "2025-09-15"),
        (2, "Bob", "2025-09-10")
    ]
    html = generate_html_table(columns, rows)
    assert "<table" in html
    assert "Alice" in html
    assert "Bob" in html

@patch("visa_check.psycopg2.connect")
def test_fetch_expiring_visas(mock_connect):
    mock_conn = MagicMock()
    mock_cursor = MagicMock()
    mock_connect.return_value = mock_conn
    mock_conn.cursor.return_value = mock_cursor
    mock_cursor.fetchall.return_value = [(1, "Alice", "2025-09-15")]
    mock_cursor.description = [("id",), ("name",), ("visa_expiry_date",)]
    rows, columns = fetch_expiring_visas()
    assert len(rows) == 1
    assert columns == ["id", "name", "visa_expiry_date"]

@patch("visa_check.SendGridAPIClient.send")
def test_send_email_alert_success(mock_send):
    mock_send.return_value = MagicMock(status_code=202)
    result = send_email_alert(
        from_email="test@example.com",
        to_emails=["recipient@example.com"],
        html_content="<p>Test</p>",
        sg_api_key="fake-key"
    )
    assert result is True

@patch("visa_check.SendGridAPIClient.send")
def test_send_email_alert_failure(mock_send):
    mock_send.side_effect = Exception("Send failed")
    result = send_email_alert(
        from_email="test@example.com",
        to_emails=["recipient@example.com"],
        html_content="<p>Test</p>",
        sg_api_key="fake-key"
    )
    assert result is False

def test_send_email_alert_empty_content():
    result = send_email_alert(
        from_email="test@example.com",
        to_emails=["recipient@example.com"],
        html_content="",
        sg_api_key="fake-key"
    )
    assert result is False

if __name__ == "__main__":
    unittest.main()
