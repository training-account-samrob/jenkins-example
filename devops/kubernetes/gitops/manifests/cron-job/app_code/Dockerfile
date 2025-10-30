FROM python:3.9-slim
WORKDIR /app
COPY visa_check.py /app
RUN pip install psycopg2-binary sendgrid
CMD ["python", "visa_check.py"]