//
// Created by reveny on 5/30/24.
//
#pragma once

#include <ELFUtil.hpp>
#include <string>

namespace Utility {
    int GetSDKVersion();

    std::string GetProcessName();
    std::string GetSystemProperty(const char *property);
    bool IsProbablyEmulator();
    ELFParser::MachineType GetMachineType();
}
