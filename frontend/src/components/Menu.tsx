import {Anchor, Center, Group, Image, Space, Text} from "@mantine/core";
import {Link, NavLink} from "react-router-dom";
import React from "react";
import {IUser} from "../model/model";
import {Logout, UserCircle} from "tabler-icons-react";
import {useCookies} from "react-cookie";
import {getUser} from "./utils/userUtils";
import axios from "axios";
import {noop} from "../util";

export const Menu: React.FC = () => {
    const user: IUser | null = getUser()
    const [cookies, setCookie, removeCookie] = useCookies();

    const logOut = () => {
        removeCookie('session');
        localStorage.removeItem('user');
        axios.post("/api/logout").then(noop);
    }

    return <Center>
        <Group id="menu">
            <NavLink to={"/"}>
                <Image
                    id="logo"
                    src={require('../img/logo-dark-48px.png')}
                    height={48}
                    width='auto'
                    alt=''
                /></NavLink>
            <Space h='md'/>
            <Text size='md'>
                <Link to={"/about"}>О проекте</Link>&nbsp;·&nbsp;
                <Link to={"/manual"}>Как пользоваться</Link>
                {user && <>&nbsp;·&nbsp;</> }
                {user && <><Link to={"/create"}>Создать карту</Link>&nbsp;·&nbsp;</> }
                {user && <><Link to={"/maplist"}>Мои карты</Link></> }
                &nbsp;&nbsp;|&nbsp;&nbsp;
                <span className="profile">
                {
                    !!user
                        ? <>
                            <UserCircle/>&nbsp;<Link to={"/profile"}>{user.email}</Link>&nbsp;
                            <Link to="/logout" onClick={logOut}><Logout/></Link>
                        </>
                        : <><Link to="/login">Вход</Link>&nbsp;·&nbsp;<Link to="/registration">Регистрация</Link></>
                }
                </span>
            </Text>
        </Group>
    </Center>
}