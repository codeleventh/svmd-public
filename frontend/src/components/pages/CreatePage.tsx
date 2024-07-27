import React, {useState} from 'react'
import {Accordion, Anchor, Button, Card, Center, SimpleGrid, Text, TextInput, Title} from "@mantine/core";
import {Help, Link as LinkIcon} from "tabler-icons-react";
import axios from "axios";
import {IApiResponse} from "../../model/rests";
import {ErrorTemplate} from "../ErrorTemplate";
import {isEmpty, pipe} from "ramda";
import {errorHandler} from "../utils/apiUtils";
import {IUser} from "../../model/model";
import {getUser} from "../utils/userUtils";

export const CreatePage: React.FC = () => {
    const [url, setUrl] = useState('')
    const user: IUser | null = getUser()

    const urlRegex = /(?<=^https:\/\/docs.google.com\/spreadsheets\/d\/e\/)2PACX-[-_a-zA-Z0-9]{80}(?=\/)?/
    const isValid = (url: string) => urlRegex.test(url)
    const extractSpreadsheetId = (url: string) => url.match(urlRegex)?.at(0) ?? ""

    const [response, setResponse] = useState<IApiResponse<string>>()
    const onSubmit = () => axios
        .post(`/api/meta`, {spreadsheetId: extractSpreadsheetId(url)})
        .then((response) => response.data)
        .then((response) => setResponse(response))
        .catch(pipe(errorHandler, setResponse))
    // TODO: a bit of SOLID, plz

    return <>
        <Title>Создание карты</Title>
        {
        !user
            ? <ErrorTemplate errors={["Пользователь не авторизован"]}/>
            : <>
                {!response &&
                <SimpleGrid>
                    <TextInput
                        label="Ссылка на публичную таблицу в Google Spreadsheet"
                        required
                        icon={<LinkIcon/>}
                        value={url}
                        error={!isEmpty(url) && !isValid(url) ? "Неправильная ссылка. Убедитесь, что таблица опубликована" : false}
                        onChange={(e) => setUrl(e.target.value)}
                    />
                    <Accordion iconPosition="left" disableIconRotation>
                        <Accordion.Item icon={<Help/>} label="Как опубликовать таблицу и получить ссылку?">
                            <Center>
                                <video muted loop controls autoPlay width={'500px'}>
                                    VIDEO
                                    {/*<source src={require('../../vid/howto.webm')} type="video/webm"/>*/}
                                </video>
                            </Center>
                        </Accordion.Item>
                    </Accordion>
                    <Button onClick={onSubmit} disabled={isEmpty(url) || !isValid(url)}>Сохранить</Button>
                </SimpleGrid>
                }
                {response && !response.success && <ErrorTemplate errors={response.errors}/>}
                {response && response.success &&
                <Center p="xl">
                    <Card shadow="md">
                        <Text>Готово! Карта создана, таблицу можно заполнять данными.</Text>
                        <Text>Используйте <Anchor href={`/${response.body}`}>эту ссылку</Anchor> для публикации в интернете
                            (а <Anchor href={`/${response.body}?iframe`}>эту</Anchor> — для встраивания через
                            iframe).</Text>
                        <Text>Также можно <Anchor href={`/${response.body}/edit`}>отредактировать настройки
                            карты</Anchor>.</Text>
                    </Card>
                </Center>
                }
            </>
    }</>
}
