FROM  python:2.7.13-alpine

MAINTAINER Stefan Negru <stefan.negru@helsinki.fi>

EXPOSE 5899

WORKDIR /app
COPY . /app

RUN pip install --no-cache-dir --upgrade pip \
    && pip install --no-cache-dir -r requirements.txt

RUN mkdir -p /app/logs

CMD python gm_cronjob.py
